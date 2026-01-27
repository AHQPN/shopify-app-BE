package org.chatapp.customshopify.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HideReason {
    FAKE("Fake"),
    DUPLICATED("Duplicated"),
    SPAM("Spam"),
    UNRELATED_TO_PRODUCT("Unrelated to product"),
    LEGAL("Legal"),
    INAPPROPRIATE_LANGUAGE("Inappropriate language"),
    FOREIGN_LANGUAGE("Foreign language"),
    PRIVATE_INFORMATION("Private information"),
    MISLEADING("Misleading");

    private final String displayValue;
}
