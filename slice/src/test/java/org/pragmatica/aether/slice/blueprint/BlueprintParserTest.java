package org.pragmatica.aether.slice.blueprint;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for BlueprintParser.
 *
 * <p>Note: Routing is no longer parsed in blueprints - routes are self-registered
 * by slices during activation via RouteRegistry.
 */
class BlueprintParserTest {

    @Nested
    class SuccessCases {

        @Test
        void parse_succeeds_withCompleteValidDsl() {
            var dsl = """
                    # Blueprint: my-app:1.0.0

                    [slices]
                    org.example:user-service:1.0.0 = 2
                    org.example:order-service:1.0.0 = 3
                    org.example:payment-service:1.0.0
                    """;

            BlueprintParser.parse(dsl)
                           .onFailureRun(Assertions::fail)
                           .onSuccess(blueprint -> {
                               assertThat(blueprint.id().asString()).isEqualTo("my-app:1.0.0");
                               assertThat(blueprint.slices()).hasSize(3);

                               var userService = blueprint.slices().get(0);
                               assertThat(userService.instances()).isEqualTo(2);
                               assertThat(userService.artifact()
                                                     .asString()).isEqualTo("org.example:user-service:1.0.0");

                               var orderService = blueprint.slices().get(1);
                               assertThat(orderService.instances()).isEqualTo(3);

                               var paymentService = blueprint.slices().get(2);
                               assertThat(paymentService.instances()).isEqualTo(1);
                           });
        }

        @Test
        void parse_succeeds_withMinimalDsl() {
            var dsl = """
                    # Blueprint: minimal:1.0.0

                    [slices]
                    org.example:service:1.0.0
                    """;

            BlueprintParser.parse(dsl)
                           .onFailureRun(Assertions::fail)
                           .onSuccess(blueprint -> {
                               assertThat(blueprint.id().asString()).isEqualTo("minimal:1.0.0");
                               assertThat(blueprint.slices()).hasSize(1);
                           });
        }

        @Test
        void parse_succeeds_withCommentsAndBlankLines() {
            var dsl = """
                    # Blueprint: with-comments:1.0.0

                    # Slice definitions
                    [slices]
                    org.example:service-a:1.0.0 = 2  # Two instances

                    # Another service
                    org.example:service-b:1.0.0
                    """;

            BlueprintParser.parse(dsl)
                           .onFailureRun(Assertions::fail)
                           .onSuccess(blueprint -> {
                               assertThat(blueprint.slices()).hasSize(2);
                               assertThat(blueprint.slices().get(0).instances()).isEqualTo(2);
                               assertThat(blueprint.slices().get(1).instances()).isEqualTo(1);
                           });
        }

        @Test
        void parse_succeeds_withQualifiedVersions() {
            var dsl = """
                    # Blueprint: qualified:1.0.0

                    [slices]
                    org.example:service:1.0.0-SNAPSHOT = 2
                    org.example:other:2.0.0-alpha1
                    """;

            BlueprintParser.parse(dsl)
                           .onFailureRun(Assertions::fail)
                           .onSuccess(blueprint -> {
                               assertThat(blueprint.slices()).hasSize(2);
                               assertThat(blueprint.slices().get(0).artifact().asString())
                                       .isEqualTo("org.example:service:1.0.0-SNAPSHOT");
                               assertThat(blueprint.slices().get(1).artifact().asString())
                                       .isEqualTo("org.example:other:2.0.0-alpha1");
                           });
        }

        @Test
        void parseFile_succeeds_withValidFile() throws IOException {
            var tempFile = Files.createTempFile("blueprint", ".dsl");
            try {
                var dsl = """
                        # Blueprint: from-file:1.0.0

                        [slices]
                        org.example:service:1.0.0
                        """;
                Files.writeString(tempFile, dsl);

                BlueprintParser.parseFile(tempFile)
                               .onFailureRun(Assertions::fail)
                               .onSuccess(blueprint -> {
                                   assertThat(blueprint.id().asString()).isEqualTo("from-file:1.0.0");
                                   assertThat(blueprint.slices()).hasSize(1);
                               });
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        void parse_ignoresUnknownSections() {
            var dsl = """
                    # Blueprint: with-unknown:1.0.0

                    [slices]
                    org.example:service:1.0.0

                    [routing:http]
                    GET:/api/test => service:test(body)
                    """;

            BlueprintParser.parse(dsl)
                           .onFailureRun(Assertions::fail)
                           .onSuccess(blueprint -> {
                               assertThat(blueprint.id().asString()).isEqualTo("with-unknown:1.0.0");
                               assertThat(blueprint.slices()).hasSize(1);
                               // Routing section is ignored
                           });
        }
    }

    @Nested
    class FailureCases {

        @Test
        void parse_fails_whenMissingHeader() {
            var dsl = """
                    [slices]
                    org.example:service:1.0.0
                    """;

            BlueprintParser.parse(dsl)
                           .onSuccessRun(Assertions::fail)
                           .onFailure(cause ->
                                              assertThat(cause.message()).contains("Missing blueprint header")
                                     );
        }

        @Test
        void parse_fails_whenInvalidBlueprintId() {
            var dsl = """
                    # Blueprint: INVALID_ID

                    [slices]
                    org.example:service:1.0.0
                    """;

            BlueprintParser.parse(dsl)
                           .onSuccessRun(Assertions::fail)
                           .onFailure(cause ->
                                              assertThat(cause.message()).contains("Invalid")
                                     );
        }

        @Test
        void parse_fails_whenInvalidSliceFormat() {
            var dsl = """
                    # Blueprint: test:1.0.0

                    [slices]
                    not-an-artifact
                    """;

            BlueprintParser.parse(dsl)
                           .onSuccessRun(Assertions::fail)
                           .onFailure(cause ->
                                              assertThat(cause.message()).contains("Invalid")
                                     );
        }

        @Test
        void parse_fails_whenInvalidInstanceCount() {
            var dsl = """
                    # Blueprint: test:1.0.0

                    [slices]
                    org.example:service:1.0.0 = -1
                    """;

            BlueprintParser.parse(dsl)
                           .onSuccessRun(Assertions::fail)
                           .onFailure(cause ->
                                              assertThat(cause.message()).contains("positive")
                                     );
        }

        @Test
        void parseFile_fails_whenFileDoesNotExist() {
            var nonExistentFile = Path.of("/nonexistent/blueprint.dsl");

            BlueprintParser.parseFile(nonExistentFile)
                           .onSuccessRun(Assertions::fail)
                           .onFailure(cause ->
                                              assertThat(cause.message()).contains("Failed to read file")
                                     );
        }
    }
}
