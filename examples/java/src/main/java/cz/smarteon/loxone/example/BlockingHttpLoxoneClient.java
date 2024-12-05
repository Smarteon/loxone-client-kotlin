package cz.smarteon.loxkt.example;

import cz.smarteon.loxkt.Command;
import cz.smarteon.loxkt.LoxoneClient;
import cz.smarteon.loxkt.LoxoneResponse;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public class BlockingHttpLoxoneClient {

    private final LoxoneClient wrappedClient;

    public BlockingHttpLoxoneClient(@NotNull final LoxoneClient wrappedClient) {
        this.wrappedClient = requireNonNull(wrappedClient);
    }

    @NotNull
    public <RESPONSE extends LoxoneResponse> Object call(@NotNull final Command<? extends RESPONSE> command) {
        try {
            return BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> wrappedClient.call(command, continuation)
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public String callRaw(@NotNull final String command) {
        try {
            return BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> wrappedClient.callRaw(command, continuation)
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> wrappedClient.close(continuation)
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
