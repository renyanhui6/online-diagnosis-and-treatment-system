package cn.edu.ncu.medical.ai;

import lombok.Data;

@Data
public class AiSanitizeMeta {
    private boolean masked;
    private boolean truncated;

    private int summaryLength;
    private int snippetLength;
    private int descriptionLength;
    private int symptomCount;
    private int symptomsTotalLength;
}
