package cz.smarteon.loxone.example;

import cz.smarteon.loxone.Command;
import cz.smarteon.loxone.LoxoneClient;
import cz.smarteon.loxone.LoxoneResponse;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
        wrappedClient.close();
    }
}
