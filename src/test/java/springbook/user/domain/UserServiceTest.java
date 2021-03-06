package springbook.user.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static springbook.user.domain.UserServiceImpl.MAX_RECOMMEND_FOR_GOLD;
import static springbook.user.domain.UserServiceImpl.MIN_LOGCOUNT_FOR_SILVER;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "file:src/main/resources/applicationContext.xml")
class UserServiceTest {
    @Autowired
    private UserDao userDao;
    @Autowired
    private UserService userService;
    @Autowired
    private UserService testUserService;

    private List<User> users;

    @BeforeEach
    void setUp() {
        users = Arrays.asList(
            new User("id1","name1","pwd1", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER-1, 0, "aa@bb.cc"),
            new User("id2","name2","pwd2", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0, "aa@bb.cc"),
            new User("id3","name3","pwd3", Level.SILVER, 60, MAX_RECOMMEND_FOR_GOLD-1, "aa@bb.cc"),
            new User("id4","name4","pwd4", Level.SILVER, 60, MAX_RECOMMEND_FOR_GOLD, "aa@bb.cc"),
            new User("id5","name5","pwd5", Level.GOLD, 100, 100, "aa@bb.cc")
        );
    }

    @Test
    void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4);
        User userWithoutLevel = users.get(0);
        userWithoutLevel.setLevel(null);

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelGet = userDao.get(userWithLevel.getId());
        User userWithoutLevelGet = userDao.get(userWithoutLevel.getId());

        assertEquals(Level.GOLD, userWithLevelGet.getLevel());
        assertEquals(Level.BASIC, userWithoutLevelGet.getLevel());
    }

    @Test
    void upgradeLevels() {
        UserServiceImpl testUserService = new UserServiceImpl();
        MockUserDao mockUserDao = new MockUserDao(users);
        MockMailSender mockMailSender = new MockMailSender();

        testUserService.setUserDao(mockUserDao);
        testUserService.setMailSender(mockMailSender);

        testUserService.upgradeLevels();

        List<User> updates = mockUserDao.getUpdates();
        assertEquals(2, updates.size());
        assertEquals(Level.SILVER, updates.get(0).getLevel());
        assertEquals(Level.GOLD, updates.get(1).getLevel());

        List<String> requests = mockMailSender.getRequests();
        assertEquals(2, requests.size());
        assertEquals(users.get(1).getEmail(), requests.get(0));
        assertEquals(users.get(3).getEmail(), requests.get(1));
    }

    @Test
    void upgradeAllOrNothing() throws Exception {
        userDao.deleteAll();
        for(User user : users)
            userDao.add(user);

        try {
            testUserService.upgradeLevels();
        } catch (Exception e) {}

        assertTrue(checkLevel(users.get(1), Level.BASIC));
        assertTrue(checkLevel(users.get(3), Level.SILVER));
    }

    private boolean checkLevel(User user, Level expected) {
        User get = userDao.get(user.getId());
        return get.getLevel().equals(expected);
    }

}