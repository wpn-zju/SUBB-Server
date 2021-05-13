package com.subb.service.controller;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.subb.service.controller.enums.EnumGender;
import com.subb.service.controller.enums.EnumMessageType;
import com.subb.service.controller.patterns.PatternChecker;
import com.subb.service.controller.patterns.exceptions.InvalidPasscodeException;
import com.subb.service.controller.patterns.exceptions.InvalidUserEmailException;
import com.subb.service.controller.patterns.exceptions.InvalidUserNameException;
import com.subb.service.controller.patterns.exceptions.InvalidUserPasswordException;
import com.subb.service.controller.response.SmallTalkResponseBody;
import com.subb.service.controller.utilities.ClientConstant;
import com.subb.service.database.DatabaseService;
import com.subb.service.database.exception.*;
import com.subb.service.database.model.account.ContactData;
import com.subb.service.database.model.account.UserData;
import com.subb.service.database.model.response.HistoryRecord;
import com.subb.service.database.model.response.Notification;
import com.subb.service.database.model.response.PrivateMessage;
import com.subb.service.database.model.site.CommentData;
import com.subb.service.database.model.site.ForumData;
import com.subb.service.database.model.site.PostData;
import com.subb.service.database.model.site.ThreadData;
import com.subb.service.tool.EmailHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("DefaultAnnotationParam")
@Controller
public class BulletinBoardController {
    public static final String SESSION_TOKEN = "SESSION_TOKEN";

    @PostMapping(ClientConstant.API_SIGN_UP)
    public ResponseEntity<?> signUp(
            HttpServletResponse response,
            @RequestParam(required = true, name = ClientConstant.SIGN_UP_EMAIL) String email,
            @RequestParam(required = true, name = ClientConstant.SIGN_UP_PASSWORD) String password,
            @RequestParam(required = true, name = ClientConstant.SIGN_UP_PASSCODE) String passcode
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
                    new SmallTalkResponseBody(200, "SMALLTALK - sign up - success"));
        } catch (UserEmailExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - sign up - email exists"));
        } catch (PasscodeException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(402, "SMALLTALK - sign up - passcode Wrong"));
        } catch (InvalidUserEmailException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(601, "SMALLTALK - sign up - invalid email address"));
        } catch (InvalidUserPasswordException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(602, "SMALLTALK - sign up - invalid password"));
        } catch (InvalidPasscodeException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(603, "SMALLTALK - sign up - invalid passcode"));
        }
    }

    @PostMapping(ClientConstant.API_RECOVER_PASSWORD)
    public ResponseEntity<?> recoverPassword(
            HttpServletResponse response,
            @RequestParam(required = true, name = ClientConstant.RECOVER_PASSWORD_EMAIL) String email,
            @RequestParam(required = true, name = ClientConstant.RECOVER_PASSWORD_PASSWORD) String password,
            @RequestParam(required = true, name = ClientConstant.RECOVER_PASSWORD_PASSCODE) String passcode
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
                    new SmallTalkResponseBody(200, "SMALLTALK - recover password - success"));
        } catch (UserEmailNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - recover password - email not exists"));
        } catch (PasscodeException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(402, "SMALLTALK - recover password - passcode wrong"));
        } catch (InvalidUserEmailException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(601, "SMALLTALK - recover password - invalid email address"));
        } catch (InvalidUserPasswordException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(602, "SMALLTALK - recover password - invalid password"));
        } catch (InvalidPasscodeException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(603, "SMALLTALK - recover password - invalid passcode"));
        }
    }

    @PostMapping(value = ClientConstant.API_SIGN_IN, params = {ClientConstant.SIGN_IN_EMAIL, ClientConstant.SIGN_IN_PASSWORD})
    public ResponseEntity<?> signIn(HttpServletResponse response,
            @RequestParam(required = true, name = ClientConstant.SIGN_IN_EMAIL) String email,
            @RequestParam(required = true, name = ClientConstant.SIGN_IN_PASSWORD) String password
    ) {
        try {
            int userId = DatabaseService.queryUserIdByEmail(email);
            UserData userData = DatabaseService.getUserDataAll(userId);
            if (DatabaseService.passwordHash(password).equals(userData.getPasswordHash())) {
                String newSession = generateSessionToken();
                DatabaseService.insertSession(userId, newSession);
                response.addCookie(new Cookie(BulletinBoardController.SESSION_TOKEN, newSession));
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - sign in - success"));
            } else {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(401, "SMALLTALK - sign in - password wrong"));
            }
        } catch (UserEmailNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(402, "SMALLTALK - sign in - user not found"));
        } catch (UserNotExistsException | NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SmallTalkResponseBody(501, "SMALLTALK - sign in - server error"));
        }
    }

    @PostMapping(ClientConstant.API_SIGN_OUT)
    public ResponseEntity<?> signOut(HttpServletRequest request, HttpServletResponse response) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - sign out - auth token not found"));
        } else {
            try {
                DatabaseService.queryUserIdBySession(session);
                DatabaseService.revokeSession(session);
                response.addCookie(new Cookie(BulletinBoardController.SESSION_TOKEN, null));
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - sign out - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - sign out - invalid auth token"));
            }
        }
    }

    @PostMapping(ClientConstant.API_REQUEST_PASSCODE)
    public ResponseEntity<?> requestPasscode(
            @RequestParam(required = true, name = ClientConstant.REQUEST_PASSCODE_EMAIL) String email
    ) {
        try {
            PatternChecker.checkUserEmail(email);
            String passcode = DatabaseService.newPasscode(email);
            EmailHelper.sendPasscode(email, "General", passcode);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - passcode request - success"));
        } catch (InvalidUserEmailException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(601, "SMALLTALK - passcode request - invalid email address"));
        } catch (UnirestException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new SmallTalkResponseBody(501, "SMALLTALK - passcode request - server error"));
        }
    }

    @PostMapping(ClientConstant.API_MODIFY_INFO)
    public ResponseEntity<?> modifyInfo(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false, name = ClientConstant.MODIFY_INFO_NICKNAME) String nickname,
            @RequestParam(required = false, name = ClientConstant.MODIFY_INFO_PASSWORD) String password,
            @RequestParam(required = false, name = ClientConstant.MODIFY_INFO_GENDER) String gender,
            @RequestParam(required = false, name = ClientConstant.MODIFY_INFO_AVATAR_LINK) String avatarLink,
            @RequestParam(required = false, name = ClientConstant.MODIFY_INFO_PERSONAL_INFO) String personalInfo
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - modify info - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                if (nickname != null) {
                    PatternChecker.checkUserName(nickname);
                    DatabaseService.modifyUserName(userId, nickname);
                }
                if (gender != null) {
                    DatabaseService.modifyUserGender(userId, EnumGender.fromString(gender));
                }
                if (avatarLink != null) {
                    DatabaseService.modifyUserAvatarLink(userId, avatarLink);
                }
                if (personalInfo != null) {
                    DatabaseService.modifyUserPersonalInfo(userId, personalInfo);
                }
                if (password != null) {
                    PatternChecker.checkUserPassword(password);
                    DatabaseService.modifyUserPassword(userId, password);
                    String newSession = generateSessionToken();
                    DatabaseService.insertSession(userId, newSession);
                    response.addCookie(new Cookie(BulletinBoardController.SESSION_TOKEN, newSession));
                }
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - modify info - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - modify info - invalid auth token"));
            } catch (InvalidUserNameException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(601, "SMALLTALK - modify info - invalid user name"));
            } catch (InvalidUserPasswordException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(602, "SMALLTALK - modify info - invalid password"));
            }
        }
    }

    @PostMapping(ClientConstant.API_NEW_THREAD)
    public ResponseEntity<?> newThread(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.NEW_THREAD_FORUM_ID) Integer forumId,
            @RequestParam(required = true, name = ClientConstant.NEW_THREAD_TITLE) String title,
            @RequestBody(required = true) String content
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
                int threadId = DatabaseService.newThread(authorId, forumId, title);
                DatabaseService.newPost(authorId, threadId, 0, content);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - new thread - success"));
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
            @RequestParam(required = false, defaultValue = "0", name = ClientConstant.NEW_POST_QUOTE_ID) Integer quoteId,
            @RequestBody(required = true) String content
    ) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - new post - auth token not found"));
        } else {
            try {
                // Todo: CHECK VALID RICH TEXT BODY
                int authorId = DatabaseService.queryUserIdBySession(session);
                if (quoteId == null) quoteId = 0;
                DatabaseService.newPost(authorId, threadId, quoteId, content);
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
            @RequestParam(required = false, defaultValue = "0", name = ClientConstant.NEW_COMMENT_QUOTE_ID) Integer quoteId,
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
                DatabaseService.newComment(authorId, postId, quoteId, commentContent);
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

    @GetMapping(ClientConstant.API_GET_FORUM)
    public ResponseEntity<?> getForum(
            @RequestParam(required = true, name = ClientConstant.GET_FORUM_FORUM_ID) Integer forumId
    ) {
        try {
            ForumData forumData = DatabaseService.getForumData(forumId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get forum - success", forumData));
        } catch (ForumNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get forum - invalid forum id"));
        }
    }

    @GetMapping(ClientConstant.API_GET_THREAD)
    public ResponseEntity<?> getThread(
            @RequestParam(required = true, name = ClientConstant.GET_THREAD_THREAD_ID) Integer threadId
    ) {
        try {
            ThreadData threadData = DatabaseService.getThreadData(threadId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get thread - success", threadData));
        } catch (ThreadNotExistException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get thread - invalid thread id"));
        }
    }

    @GetMapping(ClientConstant.API_GET_POST)
    public ResponseEntity<?> getPost(
            @RequestParam(required = true, name = ClientConstant.GET_POST_POST_ID) Integer postId
    ) {
        try {
            PostData postData = DatabaseService.getPostData(postId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get post - success", postData));
        } catch (PostNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get post - invalid post id"));
        }
    }

    @GetMapping(ClientConstant.API_GET_COMMENT)
    public ResponseEntity<?> getComment(
            @RequestParam(required = true, name = ClientConstant.GET_COMMENT_COMMENT_ID) Integer commentId
    ) {
        try {
            CommentData commentData = DatabaseService.getCommentData(commentId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get post - success", commentData));
        } catch (CommentNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get comment - invalid comment id"));
        }
    }

    @GetMapping(ClientConstant.API_GET_HOMEPAGE)
    public ResponseEntity<?> getHomepage() {
        List<ThreadData> homepage = DatabaseService.getHomepage();
        return ResponseEntity.ok().body(
                new SmallTalkResponseBody(200, "SMALLTALK - get homepage - success", homepage));
    }

    @GetMapping(ClientConstant.API_GET_FORUM_PAGE)
    public ResponseEntity<?> getForumPage(
            @RequestParam(required = true, name = ClientConstant.GET_FORUM_PAGE_FORUM_ID) Integer forumId,
            @RequestParam(required = true, name = ClientConstant.GET_FORUM_PAGE_PAGE) Integer page
    ) {
        try {
            List<ThreadData> forumPage = DatabaseService.getForumPage(forumId, page);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get forum page - success", forumPage));
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
            List<PostData> threadPage = DatabaseService.getThreadPage(threadId, page);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get thread page - success", threadPage));
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
            List<CommentData> postPage = DatabaseService.getPostPage(postId, page);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get post page - success", postPage));
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
            List<CommentData> commentPage = DatabaseService.getCommentPage(commentId, page);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get comment page - success", commentPage));
        } catch (MalformedRequestException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - get comment page - malformed request"));
        }
    }

    @GetMapping(ClientConstant.API_GET_THREAD_HISTORY)
    public ResponseEntity<?> getThreadHistory(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.GET_THREAD_HISTORY_PAGE) Integer page) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get thread history - auth token not found"));
        } else {
            try {
                int userId  = DatabaseService.queryUserIdBySession(session);
                List<ThreadData> threadHistoryData = DatabaseService.getThreadHistory(userId, page);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - get thread history - success", threadHistoryData));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - get thread history - invalid auth token"));
            }
        }
    }

    @GetMapping(ClientConstant.API_GET_POST_HISTORY)
    public ResponseEntity<?> getPostHistory(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.GET_POST_HISTORY_PAGE) Integer page) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get post history - auth token not found"));
        } else {
            try {
                int userId  = DatabaseService.queryUserIdBySession(session);
                List<PostData> postHistoryData = DatabaseService.getPostHistory(userId, page);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - get post history - success", postHistoryData));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - get post history - invalid auth token"));
            }
        }
    }

    @GetMapping(ClientConstant.API_GET_COMMENT_HISTORY)
    public ResponseEntity<?> getCommentHistory(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.GET_COMMENT_HISTORY_PAGE) Integer page) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get comment history - auth token not found"));
        } else {
            try {
                int userId  = DatabaseService.queryUserIdBySession(session);
                List<CommentData> commentHistoryData = DatabaseService.getCommentHistory(userId, page);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - get comment history - success", commentHistoryData));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - get comment history - invalid auth token"));
            }
        }
    }

    @GetMapping(ClientConstant.API_GET_BROWSING_HISTORY)
    public ResponseEntity<?> getBrowsingHistory(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.GET_BROWSING_HISTORY_PAGE) Integer page) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get browsing history - auth token not found"));
        } else {
            try {
                int userId  = DatabaseService.queryUserIdBySession(session);
                List<HistoryRecord> browsingHistoryData = DatabaseService.getBrowsingHistory(userId, page);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - get browsing history - success", browsingHistoryData));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - get browsing history - invalid auth token"));
            }
        }
    }

    @GetMapping(ClientConstant.API_LOAD_SELF)
    public ResponseEntity<?> loadSelf(HttpServletRequest request) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - load self - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                UserData userData = DatabaseService.getUserDataAll(userId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - load self - success", userData));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - load self - invalid auth token"));
            } catch (UserNotExistsException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        new SmallTalkResponseBody(501, "SMALLTALK - load self - server error"));
            }
        }
    }

    @GetMapping(value = ClientConstant.API_LOAD_USER, params = ClientConstant.LOAD_USER_USER_ID)
    public ResponseEntity<?> loadUser(
            @RequestParam(required = true, name = ClientConstant.LOAD_USER_USER_ID) int contactId
    ) {
        try {
            ContactData contactData = DatabaseService.getUserData(contactId);
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(200, "SMALLTALK - load user - success", contactData));
        } catch (UserNotExistsException e) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - load user - user not found"));
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
                DatabaseService.pushPrivateMessage(sender, receiver, content, EnumMessageType.MESSAGE_TYPE_PRIVATE);
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

    @GetMapping(ClientConstant.API_FETCH_PRIVATE_MESSAGE)
    public ResponseEntity<?> fetchPrivateMessage(HttpServletRequest request) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - fetch private message - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                List<PrivateMessage> messageData = DatabaseService.fetchPrivateMessage(userId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - fetch private message - success", messageData));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - fetch private message - invalid auth token"));
            }
        }
    }

    @PostMapping(ClientConstant.API_READ_PRIVATE_MESSAGE)
    public ResponseEntity<?> readPrivateMessage(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.READ_PRIVATE_MESSAGE_MESSAGE_ID) Integer messageId) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - read private message - auth token not found"));
        } else {
            try {
                int userId = DatabaseService.queryUserIdBySession(session);
                DatabaseService.readPrivateMessage(userId, messageId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - read private message - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - read private message - invalid auth token"));
            }
        }
    }

    @PostMapping(ClientConstant.API_ADMIN_DISABLE_ACCOUNT)
    public ResponseEntity<?> adminDisableAccount(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.ADMIN_DISABLE_ACCOUNT_USER_ID) Integer userId,
            @RequestParam(required = false, defaultValue = "0", name = ClientConstant.ADMIN_DISABLE_ACCOUNT_PERIOD) Integer period
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
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

    @GetMapping(ClientConstant.API_GET_NOTIFICATION)
    public ResponseEntity<?> getNotification(HttpServletRequest request) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - get notification - auth token not found"));
        } else {
            try {
                int userId  = DatabaseService.queryUserIdBySession(session);
                List<Notification> notificationData = DatabaseService.getNotification(userId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - get notification - success", notificationData));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - get notification - invalid auth token"));
            }
        }
    }

    @PostMapping(ClientConstant.API_READ_NOTIFICATION)
    public ResponseEntity<?> readNotification(HttpServletRequest request,
            @RequestParam(required = true, name = ClientConstant.READ_NOTIFICATION_NOTIFICATION_ID) Integer notificationId) {
        String session = readCookie(request.getCookies(), BulletinBoardController.SESSION_TOKEN);
        if (session == null) {
            return ResponseEntity.ok().body(
                    new SmallTalkResponseBody(401, "SMALLTALK - read notification - auth token not found"));
        } else {
            try {
                int userId  = DatabaseService.queryUserIdBySession(session);
                DatabaseService.readNotification(userId, notificationId);
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(200, "SMALLTALK - read notification - success"));
            } catch (SessionInvalidException | SessionExpiredException | SessionRevokedException e) {
                return ResponseEntity.ok().body(
                        new SmallTalkResponseBody(402, "SMALLTALK - read notification - invalid auth token"));
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
