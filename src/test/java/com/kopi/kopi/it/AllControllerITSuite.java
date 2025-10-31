package com.kopi.kopi.it;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        AiSuggestControllerIT.class,
        GuestOrderControllerIT.class,
        TableControllerIT.class
})
public class AllControllerITSuite {
    // empty – JUnit Platform sẽ gom và chạy 3 class trên
}
