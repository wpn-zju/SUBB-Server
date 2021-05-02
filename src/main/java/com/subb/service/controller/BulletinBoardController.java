package com.subb.service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.subb.service.controller.enums.EnumMessageType;
import com.subb.service.controller.patterns.PatternChecker;
import com.subb.service.controller.patterns.exceptions.InvalidPasscodeException;
import com.subb.service.controller.patterns.exceptions.InvalidUserEmailException;
import com.subb.service.controller.patterns.exceptions.InvalidUserNameException;
import com.subb.service.controller.patterns.exceptions.InvalidUserPasswordException;
import com.subb.service.controller.response.SmallTalkResponseBody;
import com.subb.service.controller.utilities.ClientConstant;
import com.subb.service.controller.utilities.UrlBuilder;
import com.subb.service.database.DatabaseService;
import com.subb.service.database.exception.*;
import com.subb.service.database.model.account.UserData;
import com.subb.service.database.model.site.CommentData;
import com.subb.service.database.model.site.ForumData;
import com.subb.service.database.model.site.PostData;
import com.subb.service.database.model.site.ThreadData;
import com.subb.service.tool.EmailHelper;
import com.subb.service.tool.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;

@SuppressWarnings("DefaultAnnotationParam")
@Controller
public class BulletinBoardController {
    public static final String SESSION_TOKEN = "SESSION_TOKEN";

    @PostMapping(ClientConstant.API_USER_SIGN_UP)
    public ResponseEntity<?> userSignUp(
            HttpServletResponse response,
            @RequestParam(required = true, name = ClientConstant.USER_SIGN_UP_EMAIL) String email,
            @RequestParam(required = true, name = ClientConstant.USER_SIGN_UP_PASSWORD) String password,
            @RequestParam(required = true, name = ClientConstant.USER_SIGN_UP_PASSCODE) String passcode
    ) {
        try {
            PatternChecker.checkUserEmail(email);
            PatternChecker.checkUserPassword(password);
            PatternChecker.checkPasscode(passcode);
            DatabaseService.validatePasscode(passcode, email);
            int userId = DatabaseService.newAccount(email);
            DatabaseService.modifyUserName(userId, String.format("Account %s", userId));
            DatabaseService.modifyUserPassword(userId, password);
            String newSession = generateSessionToken();
            DatabaseService.insertSession(userId, newSession);
            response.addCookie(new Cookie(BulletinBoardController.SESSION_TOKEN, newSession));
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - user sign up - success"));
        } catch (UserEmailExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - user sign up - email exists"));
        } catch (PasscodeException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(402, "SMALLTALK - user sign up - passcode Wrong"));
        } catch (InvalidUserEmailException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(601, "SMALLTALK - user sign up - invalid email address"));
        } catch (InvalidUserPasswordException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(602, "SMALLTALK - user sign up - invalid password"));
        } catch (InvalidPasscodeException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(603, "SMALLTALK - user sign up - invalid passcode"));
        }
    }

    @PostMapping(ClientConstant.API_USER_RECOVER_PASSWORD)
    public ResponseEntity<?> userRecoverPassword(
            HttpServletResponse response,
            @RequestParam(required = true, name = ClientConstant.USER_RECOVER_PASSWORD_EMAIL) String email,
            @RequestParam(required = true, name = ClientConstant.USER_RECOVER_PASSWORD_PASSWORD) String password,
            @RequestParam(required = true, name = ClientConstant.USER_RECOVER_PASSWORD_PASSCODE) String passcode
    ) {
        try {
            PatternChecker.checkUserEmail(email);
            PatternChecker.checkUserPassword(password);
            PatternChecker.checkPasscode(passcode);
            int userId = DatabaseService.queryUserIdByEmail(email);
            DatabaseService.validatePasscode(passcode, email);
            DatabaseService.modifyUserPassword(userId, password);
            String newSession = generateSessionToken();
            DatabaseService.insertSession(userId, newSession);
            response.addCookie(new Cookie(BulletinBoardController.SESSION_TOKEN, newSession));
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - user recover password - success"));
        } catch (UserEmailNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - user recover password - email not exists"));
        } catch (PasscodeException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(402, "SMALLTALK - user recover password - passcode wrong"));
        } catch (InvalidUserEmailException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(601, "SMALLTALK - user recover password - invalid email address"));
        } catch (InvalidUserPasswordException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(602, "SMALLTALK - user recover password - invalid password"));
        } catch (InvalidPasscodeException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(603, "SMALLTALK - user recover password - invalid passcode"));
        }
    }

    @PostMapping(ClientConstant.API_USER_SIGN_IN)
    public ResponseEntity<?> userSignIn(HttpServletRequest request) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - user sign in - auth token not found"));
        } else {
            try {
                DatabaseService.queryUserIdBySession(session);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - user sign in - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - user sign in - invalid auth token"));
            }
        }
    }

    @PostMapping(value = ClientConstant.API_USER_SIGN_IN, params = {ClientConstant.USER_SIGN_IN_EMAIL, ClientConstant.USER_SIGN_IN_PASSWORD})
    public ResponseEntity<?> userSignIn(HttpServletResponse response,
                                        @RequestParam(required = true, name = ClientConstant.USER_SIGN_IN_EMAIL) String email,
                                        @RequestParam(required = true, name = ClientConstant.USER_SIGN_IN_PASSWORD) String password
    ) {
        try {
            int userId = DatabaseService.queryUserIdByEmail(email);
            UserData userData = DatabaseService.getUserDataAll(userId);
            if (userData.getPasswordHash().equals(password)) {
                String newSession = generateSessionToken();
                DatabaseService.insertSession(userId, newSession);
                response.addCookie(new Cookie(BulletinBoardController.SESSION_TOKEN, newSession));
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - user sign in - success"));
            } else {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(401, "SMALLTALK - user sign in - password wrong"));
            }
        } catch (UserEmailNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(402, "SMALLTALK - user sign in - user not found"));
        } catch (UserNotExistsException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SmallTalkResponseBody(501, "SMALLTALK - user sign in - server error"));
        }
    }

    @PostMapping(ClientConstant.API_USER_SIGN_OUT)
    public ResponseEntity<?> userSignOut(HttpServletRequest request, HttpServletResponse response) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - user sign out - auth token not found"));
        } else {
            try {
                DatabaseService.queryUserIdBySession(session);
                response.addCookie(new Cookie(BulletinBoardController.SESSION_TOKEN, null));
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - user sign out - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - user sign out - invalid auth token"));
            }
        }
    }

    @PostMapping(ClientConstant.API_USER_PASSCODE_REQUEST)
    public ResponseEntity<?> passcodeRequest(
            @RequestParam(required = true, name = ClientConstant.USER_PASSCODE_REQUEST_USER_EMAIL) String email
    ) {
        try {
            PatternChecker.checkUserEmail(email);
            String passcode = DatabaseService.newPasscode(email);
            EmailHelper.sendPasscode(email, "General", passcode);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - user passcode request - success"));
        } catch (InvalidUserEmailException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(601, "SMALLTALK - user passcode request - invalid email address"));
        } catch (UnirestException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SmallTalkResponseBody(501, "SMALLTALK - user passcode request - server error"));
        }
    }

    @PostMapping(ClientConstant.API_USER_MODIFY_INFO)
    public ResponseEntity<?> userModifyInfo(
            HttpServletRequest request,
            @RequestParam(required = false, name = ClientConstant.USER_MODIFY_INFO_NAME) String userName,
            @RequestParam(required = false, name = ClientConstant.USER_MODIFY_INFO_PASSWORD) String userPassword,
            @RequestParam(required = false, name = ClientConstant.USER_MODIFY_INFO_GENDER) Integer userGender,
            @RequestParam(required = false, name = ClientConstant.USER_MODIFY_INFO_AVATAR_LINK) String userAvatarLink,
            @RequestParam(required = false, name = ClientConstant.USER_MODIFY_INFO_INFO) String userInfo
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - user modify info - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                if (userName != null) {
                    PatternChecker.checkUserName(userName);
                    DatabaseService.modifyUserName(userId, userName);
                }
                if (userPassword != null) {
                    PatternChecker.checkUserPassword(userPassword);
                    DatabaseService.modifyUserPassword(userId, userPassword);
                }
                if (userGender != null) {
                    DatabaseService.modifyUserGender(userId, userGender);
                }
                if (userAvatarLink != null) {
                    DatabaseService.modifyUserAvatarLink(userId, userAvatarLink);
                }
                if (userInfo != null) {
                    DatabaseService.modifyUserPersonalInfo(userId, userInfo);
                }
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - user modify info - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - user modify info - invalid auth token"));
            } catch (InvalidUserNameException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(601, "SMALLTALK - user modify info - invalid user name"));
            } catch (InvalidUserPasswordException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(602, "SMALLTALK - user modify info - invalid password"));
            }
        }
    }

    @PostMapping(ClientConstant.API_FETCH_OFFLINE_MESSAGE)
    public ResponseEntity<?> fetchOfflineMessage(HttpServletRequest request) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - fetch offline message - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, DatabaseService.popOfflineMessageAsList(userId).toString()));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - fetch offline message - invalid auth token"));
            }
        }
    }

    @GetMapping(ClientConstant.API_LOAD_USER)
    public ResponseEntity<?> loadSelf(HttpServletRequest request) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - load self - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, new ObjectMapper().writeValueAsString(DatabaseService.getUserDataAll(userId))));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - load self - invalid auth token"));
            } catch (UserNotExistsException | JsonProcessingException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new SmallTalkResponseBody(501, "SMALLTALK - load self - server error"));
            }
        }
    }

    @GetMapping(value = ClientConstant.API_LOAD_CONTACT, params = ClientConstant.LOAD_CONTACT_CONTACT_ID)
    public ResponseEntity<?> loadUser(
            @RequestParam(required = true, name = ClientConstant.LOAD_CONTACT_CONTACT_ID) int contactId
    ) {
        try {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, new ObjectMapper().writeValueAsString(DatabaseService.getUserData(contactId))));
        } catch (UserNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - load user - user not found"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SmallTalkResponseBody(501, "SMALLTALK - load user - server error"));
        }
    }

    @GetMapping(value = ClientConstant.API_LOAD_CONTACT, params = ClientConstant.LOAD_CONTACT_BY_EMAIL_CONTACT_EMAIL)
    public ResponseEntity<?> loadUser(
            @RequestParam(required = true, name = ClientConstant.LOAD_CONTACT_BY_EMAIL_CONTACT_EMAIL) String contactEmail
    ) {
        try {
            int contactId = DatabaseService.queryUserIdByEmail(contactEmail);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, new ObjectMapper().writeValueAsString(DatabaseService.getUserData(contactId))));
        } catch (UserEmailNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - load user - user not found"));
        } catch (UserNotExistsException | JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SmallTalkResponseBody(501, "SMALLTALK - load user - server error"));
        }
    }

    @PostMapping(ClientConstant.API_FILE_ARCHIVE)
    public ResponseEntity<?> fileArchive(
            @RequestParam(required = true, name = ClientConstant.FILE_ARCHIVE_FILE_NAME) String fileName,
            @RequestParam(required = true, name = ClientConstant.FILE_ARCHIVE_FILE_LINK) String fileLink,
            @RequestParam(required = true, name = ClientConstant.FILE_ARCHIVE_FILE_UPLOADER) Integer fileUploader,
            @RequestParam(required = true, name = ClientConstant.FILE_ARCHIVE_FILE_SIZE) Integer fileSize
    ) {
        DatabaseService.newFileDescriptor(fileName, fileLink, fileUploader, fileSize);
        return ResponseEntity.ok().body(
                new SmallTalkResponseBody(200, "SMALLTALK - file archive - success"));
    }

    @PostMapping(ClientConstant.API_NEW_THREAD)
    public ResponseEntity<?> newThread(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.NEW_THREAD_FORUM_ID) Integer forumId,
            @RequestParam(required = true, name = ClientConstant.NEW_THREAD_THREAD_TITLE) String threadTitle,
            @RequestBody(required = true) String threadContent
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - new thread - auth token not found"));
        } else {
            try {
                // Todo: CHECK VALID RICH TEXT BODY
                // Todo: CHECK VALID THREAD TITLE
                int authorId = DatabaseService.queryUserIdBySession(session);
                int threadId = DatabaseService.newThread(authorId, forumId, threadTitle);
                DatabaseService.newPost(authorId, threadId, 0, threadContent);
                String threadUrl = UrlBuilder.buildThreadUrl(threadId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, threadUrl));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - new thread - invalid auth token"));
            } catch (MalformedRequestException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - new thread - invalid forum id"));
            }
        }
    }

    @PostMapping(ClientConstant.API_NEW_POST)
    public ResponseEntity<?> newPost(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.NEW_POST_THREAD_ID) Integer threadId,
            @RequestParam(required = false, name = ClientConstant.NEW_POST_REFER_TO) Integer referTo,
            @RequestBody(required = true) String postContent
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - new post - auth token not found"));
        } else {
            try {
                // Todo: CHECK VALID RICH TEXT BODY
                int authorId = DatabaseService.queryUserIdBySession(session);
                DatabaseService.newPost(authorId, threadId, referTo, postContent);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - new post - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - new post - invalid auth token"));
            } catch (MalformedRequestException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - new post - invalid thread id"));
            }
        }
    }

    @PostMapping(ClientConstant.API_NEW_COMMENT)
    public ResponseEntity<?> newComment(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.NEW_COMMENT_POST_ID) Integer postId,
            @RequestParam(required = false, name = ClientConstant.NEW_COMMENT_REFER_TO) Integer referTo,
            @RequestBody(required = true) String commentContent
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - new comment - auth token not found"));
        } else {
            try {
                // Todo: CHECK VALID CONTENT
                int authorId = DatabaseService.queryUserIdBySession(session);
                DatabaseService.newComment(authorId, postId, referTo, commentContent);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - new comment - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - new comment - invalid auth token"));
            } catch (MalformedRequestException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - new comment - invalid post id"));
            }
        }
    }

    @PostMapping(ClientConstant.API_DELETE_THREAD)
    public ResponseEntity<?> deleteThread(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.DELETE_THREAD_THREAD_ID) Integer threadId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - delete thread - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                DatabaseService.deleteThread(userId, threadId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - delete thread - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - delete thread - invalid auth token"));
            } catch (MalformedRequestException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - delete thread - invalid thread id"));
            } catch (OperationForbiddenException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(404, "SMALLTALK - delete thread - operation forbidden"));
            }
        }
    }

    @PostMapping(ClientConstant.API_DELETE_POST)
    public ResponseEntity<?> deletePost(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.DELETE_POST_POST_ID) Integer postId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - delete post - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                DatabaseService.deletePost(userId, postId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - delete post - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - delete post - invalid auth token"));
            } catch (MalformedRequestException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - delete post - invalid post id"));
            } catch (OperationForbiddenException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(404, "SMALLTALK - delete post - operation forbidden"));
            }
        }
    }

    @PostMapping(ClientConstant.API_DELETE_COMMENT)
    public ResponseEntity<?> deleteComment(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.DELETE_COMMENT_COMMENT_ID) Integer commentId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - delete comment - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                DatabaseService.deleteComment(userId, commentId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - delete comment - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - delete comment - invalid auth token"));
            } catch (MalformedRequestException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - delete comment - invalid comment id"));
            } catch (OperationForbiddenException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(404, "SMALLTALK - delete comment - operation forbidden"));
            }
        }
    }

    @PostMapping(ClientConstant.API_VOTE_THREAD)
    public ResponseEntity<?> voteThread(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.VOTE_THREAD_THREAD_ID) Integer threadId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - vote thread - auth token not found"));
        } else {
            try {
                int voterId = DatabaseService.queryUserIdBySession(session);
                DatabaseService.voteThread(voterId, threadId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - vote thread - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - vote thread - invalid auth token"));
            } catch (ThreadNotExistException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - vote thread - invalid thread id"));
            }
        }
    }

    @PostMapping(ClientConstant.API_VOTE_POST)
    public ResponseEntity<?> votePost(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.VOTE_POST_POST_ID) Integer postId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - vote post - auth token not found"));
        } else {
            try {
                int voterId  = DatabaseService.queryUserIdBySession(session);
                DatabaseService.votePost(voterId, postId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - vote post - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - vote post - invalid auth token"));
            } catch (PostNotExistsException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - vote post - invalid post id"));
            }
        }
    }

    @PostMapping(ClientConstant.API_VOTE_COMMENT)
    public ResponseEntity<?> voteComment(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.VOTE_COMMENT_COMMENT_ID) Integer commentId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - vote comment - auth token not found"));
        } else {
            try {
                int voterId  = DatabaseService.queryUserIdBySession(session);
                DatabaseService.voteComment(voterId, commentId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - vote comment - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - vote comment - invalid auth token"));
            } catch (CommentNotExistsException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - vote comment - invalid comment id"));
            }
        }
    }

    @GetMapping(ClientConstant.API_GET_BROWSING_HISTORY)
    public ResponseEntity<?> getBrowsingHistory(HttpServletRequest request) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get browsing history - auth token not found"));
        } else {
            try {
                int userId  = DatabaseService.queryUserIdBySession(session);
                JsonObject browsingHistoryData = DatabaseService.getBrowsingHistory(userId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, browsingHistoryData.toString()));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - get browsing history - invalid auth token"));
            }
        }
    }

    @GetMapping(ClientConstant.API_GET_HOMEPAGE)
    public ResponseEntity<?> getHomepage() {
        JsonObject homepage = DatabaseService.getHomepage();
        return ResponseEntity.ok().body(
                new SmallTalkResponseBody(200, homepage.toString()));
    }

    @GetMapping(ClientConstant.API_GET_FORUM_PAGE)
    public ResponseEntity<?> getForumPage(
            @RequestParam(required = true, name = ClientConstant.GET_FORUM_PAGE_FORUM_ID) Integer forumId,
            @RequestParam(required = true, name = ClientConstant.GET_FORUM_PAGE_PAGE) Integer page
    ) {
        try {
            JsonObject forumPage = DatabaseService.getForumPage(forumId, page);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, forumPage.toString()));
        } catch (MalformedRequestException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get forum page - malformed request"));
        }
    }

    @GetMapping(ClientConstant.API_GET_THREAD_PAGE)
    public ResponseEntity<?> getThreadPage(
            @RequestParam(required = true, name = ClientConstant.GET_THREAD_PAGE_THREAD_ID) Integer threadId,
            @RequestParam(required = true, name = ClientConstant.GET_THREAD_PAGE_PAGE) Integer page
    ) {
        try {
            JsonObject threadPage = DatabaseService.getThreadPage(threadId, page);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, threadPage.toString()));
        } catch (MalformedRequestException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get thread page - malformed request"));
        }
    }

    @GetMapping(ClientConstant.API_GET_POST_PAGE)
    public ResponseEntity<?> getPostPage(
            @RequestParam(required = true, name = ClientConstant.GET_POST_PAGE_POST_ID) Integer postId,
            @RequestParam(required = true, name = ClientConstant.GET_POST_PAGE_PAGE) Integer page
    ) {
        try {
            JsonObject postPage = DatabaseService.getPostPage(postId, page);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, postPage.toString()));
        } catch (MalformedRequestException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get post page - malformed request"));
        }
    }

    @GetMapping(ClientConstant.API_GET_COMMENT_PAGE)
    public ResponseEntity<?> getCommentPage(
            @RequestParam(required = true, name = ClientConstant.GET_COMMENT_PAGE_COMMENT_ID) Integer commentId,
            @RequestParam(required = true, name = ClientConstant.GET_COMMENT_PAGE_PAGE) Integer page
    ) {
        try {
            JsonObject commentPage = DatabaseService.getCommentPage(commentId, page);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, commentPage.toString()));
        } catch (MalformedRequestException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get comment page - malformed request"));
        }
    }

    @GetMapping(ClientConstant.API_GET_FORUM)
    public ResponseEntity<?> getForum(
            @RequestParam(required = true, name = ClientConstant.GET_FORUM_FORUM_ID) Integer forumId
    ) {
        try {
            ForumData forumData = DatabaseService.getForumData(forumId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, new ObjectMapper().writeValueAsString(forumData)));
        } catch (ForumNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get forum - invalid forum id"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(501, "SMALLTALK - get forum - server error"));
        }
    }

    @GetMapping(ClientConstant.API_GET_THREAD)
    public ResponseEntity<?> getThread(
            @RequestParam(required = true, name = ClientConstant.GET_THREAD_THREAD_ID) Integer threadId
    ) {
        try {
            ThreadData threadData = DatabaseService.getThreadData(threadId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, new ObjectMapper().writeValueAsString(threadData)));
        } catch (ThreadNotExistException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get thread - invalid thread id"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(501, "SMALLTALK - get thread - server error"));
        }
    }

    @GetMapping(ClientConstant.API_GET_POST)
    public ResponseEntity<?> getPost(
            @RequestParam(required = true, name = ClientConstant.GET_POST_POST_ID) Integer postId
    ) {
        try {
            PostData postData = DatabaseService.getPostData(postId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, new ObjectMapper().writeValueAsString(postData)));
        } catch (PostNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get post - invalid post id"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(501, "SMALLTALK - get post - server error"));
        }
    }

    @GetMapping(ClientConstant.API_GET_COMMENT)
    public ResponseEntity<?> getComment(
            @RequestParam(required = true, name = ClientConstant.GET_COMMENT_COMMENT_ID) Integer commentId
    ) {
        try {
            CommentData commentData = DatabaseService.getCommentData(commentId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, new ObjectMapper().writeValueAsString(commentData)));
        } catch (CommentNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get comment - invalid comment id"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(501, "SMALLTALK - get comment - server error"));
        }
    }

    @PostMapping(ClientConstant.API_ADMIN_DISABLE_ACCOUNT)
    public ResponseEntity<?> adminDisableAccount(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.ADMIN_DISABLE_ACCOUNT_USER_ID) Integer userId,
            @RequestParam(required = true, name = ClientConstant.ADMIN_DISABLE_ACCOUNT_PERIOD) Integer period
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - admin disable account - auth token not found"));
        } else {
            try {
                int adminId  = DatabaseService.queryUserIdBySession(session);
                DatabaseService.adminDisableAccount(adminId, userId, period);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - admin disable account - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - admin disable account - invalid auth token"));
            } catch (UserNotExistsException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - admin disable account - invalid user id"));
            } catch (OperationForbiddenException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(404, "SMALLTALK - admin disable account - not an administrator"));
            }
        }
    }

    @PostMapping(ClientConstant.API_ADMIN_DELETE_THREAD)
    public ResponseEntity<?> adminDeleteThread(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.ADMIN_DELETE_THREAD_THREAD_ID) Integer threadId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - admin delete thread - auth token not found"));
        } else {
            try {
                int adminId  = DatabaseService.queryUserIdBySession(session);
                DatabaseService.adminDeleteThread(adminId, threadId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - admin delete thread - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - admin delete thread - invalid auth token"));
            } catch (ThreadNotExistException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - admin delete thread - invalid thread id"));
            } catch (OperationForbiddenException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(404, "SMALLTALK - admin delete thread - not an administrator"));
            }
        }
    }

    @PostMapping(ClientConstant.API_ADMIN_DELETE_POST)
    public ResponseEntity<?> adminDeletePost(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.ADMIN_DELETE_POST_POST_ID) Integer postId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - admin delete post - auth token not found"));
        } else {
            try {
                int adminId  = DatabaseService.queryUserIdBySession(session);
                DatabaseService.adminDeletePost(adminId, postId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - admin delete post - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - admin delete post - invalid auth token"));
            } catch (PostNotExistsException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - admin delete post - invalid post id"));
            } catch (OperationForbiddenException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(404, "SMALLTALK - admin delete post - not an administrator"));
            }
        }
    }

    @PostMapping(ClientConstant.API_ADMIN_DELETE_COMMENT)
    public ResponseEntity<?> adminDeleteComment(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.ADMIN_DELETE_COMMENT_COMMENT_ID) Integer commentId
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - admin delete comment - auth token not found"));
        } else {
            try {
                int adminId  = DatabaseService.queryUserIdBySession(session);
                DatabaseService.adminDeleteComment(adminId, commentId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - admin delete comment - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - admin delete comment - invalid auth token"));
            } catch (CommentNotExistsException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - admin delete comment - invalid comment id"));
            } catch (OperationForbiddenException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(404, "SMALLTALK - admin delete comment - not an administrator"));
            }
        }
    }

    @PostMapping(ClientConstant.API_PUSH_PRIVATE_MESSAGE)
    public ResponseEntity<?> pushPrivateMessage(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.PUSH_PRIVATE_MESSAGE_RECEIVER) Integer receiver,
            @RequestParam(required = true, name = ClientConstant.PUSH_PRIVATE_MESSAGE_CONTENT) String content
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - push private message - auth token not found"));
        } else {
            try {
                int sender  = DatabaseService.queryUserIdBySession(session);
                DatabaseService.pushOfflineMessage(sender, receiver, content, EnumMessageType.MESSAGE_TYPE_PRIVATE);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - push private message - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - push private message - invalid auth token"));
            } catch (UserNotExistsException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(403, "SMALLTALK - push private message - invalid receiver id"));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static String readCookie(Cookie[] cookies, String key) {
        if (cookies == null) return null;
        return Arrays.stream(cookies).filter(cookie -> cookie.getName().equals(key)).findFirst().map(Cookie::getValue).orElse(null);
    }

    public static String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
}
