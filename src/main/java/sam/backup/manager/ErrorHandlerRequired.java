package sam.backup.manager;

import java.util.function.BiConsumer;

interface ErrorHandlerRequired {
	void setErrorHandler(BiConsumer<Object, Exception> errorHandler);
}
