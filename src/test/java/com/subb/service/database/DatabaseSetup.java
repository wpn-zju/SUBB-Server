package com.subb.service.database;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class DatabaseSetup {
    @BeforeSuite
    public void beforeSuite() throws Exception {
        DatabaseService.reset();
    }

    @AfterSuite
    public void afterSuite() {

    }

    @Test
    public void DummyTest() {

    }
}
