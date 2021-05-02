package com.subb.service.database;

import com.subb.service.database.exception.UserEmailExistsException;
import com.subb.service.database.exception.UserEmailNotExistsException;
import com.subb.service.database.exception.UserNotExistsException;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class DatabaseServiceTest {

    private final String userEmailFake = "TestUserFake@peinanweng.com";
    private final String userEmail1 = "TestUser1@peinanweng.com";
    private final String userEmail2 = "TestUser2@peinanweng.com";
    private final String userEmail3 = "TestUser3@peinanweng.com";
    private final String userEmail4 = "TestUser4@peinanweng.com";
    private final String userEmail5 = "TestUser5@peinanweng.com";
    private final String userEmail6 = "TestUser6@peinanweng.com";
    private final String userEmail7 = "TestUser7@peinanweng.com";
    private final String userEmail8 = "TestUser8@peinanweng.com";
    private final String userEmail9 = "TestUser9@peinanweng.com"; // late create
    private final String userEmail10 = "TestUser10@peinanweng.com";
    private final String userEmail11 = "TestUser11@peinanweng.com";
    private int userId1 = 0;
    private int userId2 = 0;
    private int userId3 = 0;
    private int userId4 = 0;
    private int userId5 = 0;
    private int userId6 = 0;
    private int userId7 = 0;
    private int userId8 = 0;
    private int userId9 = 0; // late create
    private int userId10 = 0; // for session testing
    private int userId11 = 0; // for login / logout record and online checking
    private final String groupName1 = "Group 1";
    private final String groupNameNew1 = "Group 1 New";
    private final String groupName2 = "Group 2";
    private final String groupName3 = "Group 3";
    private int groupId1 = 0;
    private int groupId2 = 0;
    private int groupId3 = 0; // late create
    private final String user1NewName = "I AM USER 1";
    private final String user1NewPassword = "EasyPassword";

    private final String sampleMessageContent = "Hello World!";

    @BeforeSuite
    public void beforeSuite() throws Exception {
        DatabaseService.reset();
        userId1 = DatabaseService.newAccount(userEmail1);
        userId2 = DatabaseService.newAccount(userEmail2);
        userId3 = DatabaseService.newAccount(userEmail3);
        userId4 = DatabaseService.newAccount(userEmail4);
        userId5 = DatabaseService.newAccount(userEmail5);
        userId6 = DatabaseService.newAccount(userEmail6);
        userId7 = DatabaseService.newAccount(userEmail7);
        userId8 = DatabaseService.newAccount(userEmail8);
        userId10 = DatabaseService.newAccount(userEmail10);
        userId11 = DatabaseService.newAccount(userEmail11);
        assertNotEquals(userId1, 0);
        assertNotEquals(userId2, 0);
        assertNotEquals(userId3, 0);
        assertNotEquals(userId4, 0);
        assertNotEquals(userId5, 0);
        assertNotEquals(userId6, 0);
        assertNotEquals(userId7, 0);
        assertNotEquals(userId8, 0);
        assertNotEquals(userId10, 0);
        assertNotEquals(userId11, 0);
        assertNotEquals(groupId1, 0);
        assertNotEquals(groupId2, 0);
    }

    @AfterSuite
    public void afterSuite() {
        DatabaseService.reset();
    }

    @Test
    public void testCheckUserByEmail() {
        assertFalse(DatabaseService.hasUserWithEmail(userEmailFake));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail1));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail2));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail3));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail4));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail5));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail6));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail7));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail8));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail10));
        assertTrue(DatabaseService.hasUserWithEmail(userEmail11));
    }

    @Test
    public void testQueryUserIdByEmail() throws UserEmailNotExistsException {
        assertEquals(userId1, DatabaseService.queryUserIdByEmail(userEmail1));
        assertEquals(userId2, DatabaseService.queryUserIdByEmail(userEmail2));
        assertEquals(userId3, DatabaseService.queryUserIdByEmail(userEmail3));
        assertEquals(userId4, DatabaseService.queryUserIdByEmail(userEmail4));
        assertEquals(userId5, DatabaseService.queryUserIdByEmail(userEmail5));
        assertEquals(userId6, DatabaseService.queryUserIdByEmail(userEmail6));
        assertEquals(userId7, DatabaseService.queryUserIdByEmail(userEmail7));
        assertEquals(userId8, DatabaseService.queryUserIdByEmail(userEmail8));
    }

    @Test(expectedExceptions = UserEmailNotExistsException.class)
    public void testQueryUserIdByEmailUserNotExists() throws UserEmailNotExistsException {
        DatabaseService.queryUserIdByEmail(userEmailFake);
    }

    @Test
    public void testNewAccount() throws UserEmailExistsException {
        userId9 = DatabaseService.newAccount(userEmail9);
    }

    @Test(expectedExceptions = UserEmailExistsException.class)
    public void testNewAccountConflict() throws UserEmailExistsException {
        userId9 = DatabaseService.newAccount(userEmail1);
    }

    @Test
    public void testModifyUserName() throws UserNotExistsException {
        DatabaseService.modifyUserName(userId1, user1NewName);
        assertEquals(user1NewName, DatabaseService.getUserDataAll(userId1).getNickname());
    }

    @Test
    public void testModifyPassword() throws UserNotExistsException {
        DatabaseService.modifyUserPassword(userId1, user1NewPassword);
        assertEquals(user1NewPassword, DatabaseService.getUserDataAll(userId1).getPasswordHash());
    }

    @Test
    public void testGetUserInfo() {
    }

    @Test
    public void testGetGroupInfo() {
    }

    @Test
    public void testGetRequestInfo() {
    }
}
