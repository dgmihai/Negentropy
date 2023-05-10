package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.TaskTestTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class TimeEstimateTest  extends TaskTestTemplate {

    @BeforeAll
    public void setup() {
        init();
    }


}
