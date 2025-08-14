package solid.humank.genaidemo.testutils.handlers;

/** 測試異常處理器 用於統一處理測試過程中的異常捕獲和驗證 */
public class TestExceptionHandler {

    private Exception capturedException;

    /** 捕獲異常 */
    public void captureException(Exception exception) {
        this.capturedException = exception;
    }

    /** 檢查是否有捕獲到異常 */
    public boolean hasException() {
        return capturedException != null;
    }

    /** 獲取捕獲的異常 */
    public Exception getCapturedException() {
        return capturedException;
    }

    /** 驗證是否拋出了預期的異常 */
    public void expectException(String expectedMessage) {
        if (capturedException == null) {
            throw new AssertionError("Expected exception was not thrown");
        }

        if (expectedMessage != null && !capturedException.getMessage().contains(expectedMessage)) {
            throw new AssertionError(
                    String.format(
                            "Expected error message to contain: '%s', but was: '%s'",
                            expectedMessage, capturedException.getMessage()));
        }
    }

    /** 驗證是否拋出了指定類型的異常 */
    public void expectException(Class<? extends Exception> expectedType) {
        if (capturedException == null) {
            throw new AssertionError("Expected exception was not thrown");
        }

        if (!expectedType.isInstance(capturedException)) {
            throw new AssertionError(
                    String.format(
                            "Expected exception of type %s, but was %s",
                            expectedType.getSimpleName(),
                            capturedException.getClass().getSimpleName()));
        }
    }

    /** 驗證是否拋出了指定類型和訊息的異常 */
    public void expectException(Class<? extends Exception> expectedType, String expectedMessage) {
        expectException(expectedType);
        expectException(expectedMessage);
    }

    /** 重置異常處理器 */
    public void reset() {
        this.capturedException = null;
    }

    /** 獲取異常訊息，如果沒有異常則返回null */
    public String getExceptionMessage() {
        return capturedException != null ? capturedException.getMessage() : null;
    }

    /** 獲取最後一個異常 */
    public Exception getLastException() {
        return capturedException;
    }

    /** 處理異常 */
    public void handleException(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            captureException(e);
        }
    }

    /** 處理異常並返回結果 */
    public <T> T handleExceptionWithReturn(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (Exception e) {
            captureException(e);
            return null;
        }
    }
}
