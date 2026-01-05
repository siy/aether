package org.pragmatica.aether.ttm;

import org.pragmatica.aether.config.TTMConfig;
import org.pragmatica.aether.controller.ControllerConfig;
import org.pragmatica.aether.metrics.MinuteAggregator;
import org.pragmatica.aether.ttm.model.TTMForecast;
import org.pragmatica.aether.ttm.model.TTMPredictor;
import org.pragmatica.consensus.leader.LeaderNotification.LeaderChange;
import org.pragmatica.lang.Option;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TTMManagerImpl implements TTMManager {
    private static final Logger log = LoggerFactory.getLogger(TTMManagerImpl.class);

    private final TTMConfig config;
    private final TTMPredictor predictor;
    private final ForecastAnalyzer analyzer;
    private final MinuteAggregator aggregator;
    private final Supplier<ControllerConfig> controllerConfigSupplier;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                                                                                                      var thread = new Thread(r,
                                                                                                                              "ttm-manager");
                                                                                                      thread.setDaemon(true);
                                                                                                      return thread;
                                                                                                  });

    private final AtomicReference<ScheduledFuture< ? >> evaluationTask = new AtomicReference<>();
    private final AtomicReference<TTMForecast> currentForecast = new AtomicReference<>();
    private final AtomicReference<TTMState> state = new AtomicReference<>(TTMState.STOPPED);
    private final CopyOnWriteArrayList<Consumer<TTMForecast>> callbacks = new CopyOnWriteArrayList<>();

    TTMManagerImpl(TTMConfig config,
                   TTMPredictor predictor,
                   ForecastAnalyzer analyzer,
                   MinuteAggregator aggregator,
                   Supplier<ControllerConfig> controllerConfigSupplier) {
        this.config = config;
        this.predictor = predictor;
        this.analyzer = analyzer;
        this.aggregator = aggregator;
        this.controllerConfigSupplier = controllerConfigSupplier;
    }

    @Override
    public void onLeaderChange(LeaderChange leaderChange) {
        if (leaderChange.localNodeIsLeader()) {
            log.info("Node became leader, starting TTM evaluation");
            startEvaluation();
        }else {
            log.info("Node is no longer leader, stopping TTM evaluation");
            stopEvaluation();
        }
    }

    @Override
    public Option<TTMForecast> currentForecast() {
        return Option.option(currentForecast.get());
    }

    @Override
    public TTMState state() {
        return state.get();
    }

    @Override
    public void onForecast(Consumer<TTMForecast> callback) {
        callbacks.add(callback);
    }

    @Override
    public TTMConfig config() {
        return config;
    }

    @Override
    public void stop() {
        stopEvaluation();
        predictor.close();
        scheduler.shutdown();
        try{
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread()
                  .interrupt();
        }
    }

    private void startEvaluation() {
        stopEvaluation();
        state.set(TTMState.RUNNING);
        var task = scheduler.scheduleAtFixedRate(
        this::runEvaluation, config.evaluationIntervalMs(), config.evaluationIntervalMs(), TimeUnit.MILLISECONDS);
        evaluationTask.set(task);
        log.info("TTM evaluation started with interval {}ms", config.evaluationIntervalMs());
    }

    private void stopEvaluation() {
        state.set(TTMState.STOPPED);
        var existing = evaluationTask.getAndSet(null);
        if (existing != null) {
            existing.cancel(false);
            log.info("TTM evaluation stopped");
        }
    }

    private void runEvaluation() {
        try{
            // Check if we have enough data
            int available = aggregator.aggregateCount();
            int required = config.inputWindowMinutes();
            if (available < required / 2) {
                // Need at least half the window to make meaningful predictions
                log.debug("Insufficient data for TTM: {} minutes available, {} required", available, required);
                return;
            }
            // Get input data (zero-padded if insufficient)
            float[][] input = aggregator.toTTMInput(required);
            // Run prediction
            predictor.predict(input)
                     .onSuccess(this::processPrediction)
                     .onFailure(cause -> {
                                    log.error("TTM prediction failed: {}",
                                              cause.message());
                                    state.set(TTMState.ERROR);
                                });
        } catch (Exception e) {
            log.error("TTM evaluation error: {}", e.getMessage(), e);
            state.set(TTMState.ERROR);
        }
    }

    private void processPrediction(float[] predictions) {
        var recentHistory = aggregator.recent(config.inputWindowMinutes());
        var controllerConfig = controllerConfigSupplier.get();
        var forecast = analyzer.analyze(predictions, predictor.lastConfidence(), recentHistory, controllerConfig);
        currentForecast.set(forecast);
        log.debug("TTM forecast: recommendation={}, confidence={}",
                  forecast.recommendation()
                          .getClass()
                          .getSimpleName(),
                  forecast.confidence());
        // Notify callbacks
        for (var callback : callbacks) {
            try{
                callback.accept(forecast);
            } catch (Exception e) {
                log.warn("Forecast callback error: {}", e.getMessage());
            }
        }
        state.set(TTMState.RUNNING);
    }
}
