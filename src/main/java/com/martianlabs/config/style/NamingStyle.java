package com.martianlabs.config.style;

import com.google.common.base.CaseFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NamingStyle {
    CAMEL_CASE(CaseFormat.LOWER_CAMEL),
    UNDERSCORE(CaseFormat.LOWER_UNDERSCORE),
    HYPHEN(CaseFormat.LOWER_HYPHEN);

    private final CaseFormat caseFormat;

    public String format(String methodName) {
        return CaseFormat.UPPER_CAMEL.to(this.caseFormat, methodName.replace("get", "").replace("set", ""));
    }
}
