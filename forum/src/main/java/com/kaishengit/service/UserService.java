package com.kaishengit.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kaishengit.dao.UserDao;
import com.kaishengit.entity.User;
import com.kaishengit.util.Config;
import com.kaishengit.util.EmailUtil;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UserService {

    private UserDao userDao = new UserDao();

    //发送激活邮件的TOKEN缓存
    private static Cache<String,String> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .build();



    /**
     * 校验用户名是否被占用
     * @param username
     * @return
     */
    public boolean validateUserName(String username) {
        //注意保留用户名(名人或其他的名字)
        String name = Config.get("no.signup.usernames");
        List<String> nameList = Arrays.asList(name.split(","));
        if(nameList.contains(username)) {
            return false;
        }
        return  userDao.findByUserName(username) == null;
    }

    public User findByEmail(String email) {
        return userDao.findByEmail(email);
    }

    /**
     * 新用户注册
     * @param username
     * @param password
     * @param email
     * @param phone
     */
    public void saveNewUser(String username, String password, String email, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setAvatar(User.DEFAULT_AVATAR_NAME);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(DigestUtils.md5Hex(Config.get("user.password.salt") + password));
        user.setState(User.USERSTATE_UNACTIVE);

        userDao.save(user);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //给用户发送激活邮件
                String uuid = UUID.randomUUID().toString();
                String url = "http://bbs.kaishengit.com/user/active?_="+uuid;
                //放入缓存等待6个小时
                cache.put(uuid,username);

                String html ="<h3>Dear "+username+":</h3>请点击<a href='"+url+"'>该链接</a>去激活你的账号. <br> 凯盛软件";

                EmailUtil.sendHtmlEmail(email,"用户激活邮件",html);
            }
        });
        thread.start();
    }
}