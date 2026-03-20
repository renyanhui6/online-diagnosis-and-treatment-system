package cn.edu.ncu.medical.ai;

import lombok.Getter;

@Getter
public class AiSanitizeResult<T> {
    private final T request;
    private final AiSanitizeMeta meta;

    public AiSanitizeResult(T request, AiSanitizeMeta meta) {
        this.request = request;
        this.meta = meta;
    }
}
