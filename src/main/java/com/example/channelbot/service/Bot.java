package com.example.channelbot.service;

import com.example.channelbot.config.Config;
import com.example.channelbot.model.*;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMembersCount;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class Bot extends TelegramLongPollingBot {
    @Autowired
    Config config;

    @Autowired
    private MailNowRepository mailNowRepository;
    @Autowired
    private UserRepository userRepository;

    public Bot(Config config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    public MailNow mail;

    public MailNow getMail() {
        return mail;
    }

    public void setMail(MailNow mail) {
        this.mail = mail;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        var mailnow=mailNowRepository.findAll();
        if(mailnow.size()!=0){
            for (MailNow mailNow1:mailnow) {
                var Users= userRepository.findAll();
                for(User user:Users){
                    if(mailNow1.getDoc().equals("Yes") && mailNow1.getChannel().equals("No")){
                        SendDoc(user.getChatid(),mailNow1, mailNow1.getBody(), user);
                    } else if (mailNow1.getPhoto().equals("Yes") && mailNow1.getChannel().equals("No")) {
                        SendPic(user.getChatid(),mailNow1, mailNow1.getBody(), user);
                    } else if (mailNow1.getChannel().equals("Yes") && mailNow1.getDoc().equals("Yes")) {
                        setMail(mailNow1);
                        check(mailNow1, user, "CheckDoc");
                    } else if (mailNow1.getPhoto().equals("Yes") && mailNow1.getChannel().equals("Yes")) {
                        check(mailNow1, user, "CheckPic");
                    } else if (mailNow1.getChannel().equals("Yes")) {
                        check(mailNow1, user, "Check");
                    } else{
                        sendTextMessage(user.getChatid(), mailNow1.getBody());
                    }
                }
                mailNowRepository.deleteById(mailNow1.getId());
            }
        }
        else{
            if (update.hasMessage() && update.getMessage().hasText()) {
                Message message = update.getMessage();
                long chatId = message.getChatId();
                String channelUsername = "@test_progra";
                startCommand(chatId, update.getMessage().getChat().getFirstName());
            } else if (update.hasCallbackQuery()) {
                //берем его колбакдата для проверки
                String callBack=update.getCallbackQuery().getData();
                //получаем айди сообщение
                long messageId=update.getCallbackQuery().getMessage().getMessageId();
                //получение айди ползователя
                long chatId=update.getCallbackQuery().getMessage().getChatId();
                String channelUsername = "@test_progra";
                if(callBack.equals("CheckDoc")){
                    if(checkSubscription(chatId, channelUsername)){
                        SendDocCheck(chatId, getMail(), getMail().getBodyAfter());
                    }
                    else{
                        NoSub(chatId, "CheckDoc");
                    }
                } else if (callBack.equals("CheckPic")) {
                    if(checkSubscription(chatId, channelUsername)){
                        SendPicCheck(chatId, getMail(), getMail().getBodyAfter());
                    }else{
                        NoSub(chatId, "CheckPic");
                    }
                } else if (callBack.equals("Check")) {
                    if(checkSubscription(chatId, channelUsername)){
                        sendTextMessage(chatId, getMail().getBodyAfter());
                    }else{
                        NoSub(chatId, "Check");
                    }
                }
            }
        }
    }
    private boolean checkSubscription(long chatId, String channelUsername) {
        GetChatMembersCount getChatMembersCount = new GetChatMembersCount();
        getChatMembersCount.setChatId(channelUsername);
        ChatMember chatMember = getChatMember(channelUsername, chatId);
        if (chatMember != null)
            return chatMember.getStatus().equals("member") || chatMember.getStatus().equals("administrator");
        return false;
    }

    private ChatMember getChatMember(String channelUsername, long chatId) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(channelUsername);
        getChatMember.setUserId(chatId);

        try {
            return execute(getChatMember);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }



    private void sendTextMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startCommand(long ChatId, String name){
        String text="Hi, "+name+", nice to meet you! My name is AccountantBot," +
                " I am your personal accountant, I can" +
                " record your expenses or income. Let's start";
        sendTextMessage(ChatId, text);
        register_user(ChatId, name);
    }

    private void register_user(long ChatId, String name){
        User users=new User();
        users.setChatid(ChatId);
        users.setName(name);
        users.setTime(new Timestamp(System.currentTimeMillis()));
        userRepository.save(users);
    }
    public void SendDoc(long chatId,MailNow ma, String text, User user){
        Path path = Paths.get(ma.getDocPath());
        // Создание объекта SendDocument
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(chatId));  // chatId пользователя, которому вы хотите отправить документ
        sendDocument.setDocument(new InputFile(path.toFile()));
        sendDocument.setCaption(text);
        try{
            execute(sendDocument);
        }catch(TelegramApiException e){
            System.out.println("We have problem"+e.getMessage());
        }
    }
    public void SendPic(long chatId,MailNow ma, String text, User user){
        Path path = Paths.get(ma.getPicPath());
        SendPhoto sendPhoto=new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));
        sendPhoto.setPhoto(new InputFile(path.toFile()));
        sendPhoto.setCaption(text);
        try{
            execute(sendPhoto);
        }catch(TelegramApiException e){
            System.out.println("We have problem"+e.getMessage());
        }
    }
    
    public void SendDocCheck(long chatId,MailNow ma, String text){
        Path path = Paths.get(ma.getDocPath());
        // Создание объекта SendDocument
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(chatId));  // chatId пользователя, которому вы хотите отправить документ
        sendDocument.setDocument(new InputFile(path.toFile()));
        sendDocument.setCaption(text);
        try{
            execute(sendDocument);
        }catch(TelegramApiException e){
            System.out.println("We have problem"+e.getMessage());
        }
    }
    public void SendPicCheck(long chatId,MailNow ma, String text){
        Path path = Paths.get(ma.getPicPath());
        SendPhoto sendPhoto=new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chatId));
        sendPhoto.setPhoto(new InputFile(path.toFile()));
        sendPhoto.setCaption(text);
        try{
            execute(sendPhoto);
        }catch(TelegramApiException e){
            System.out.println("We have problem"+e.getMessage());
        }
    }

    public void check(MailNow ma, User user, String text){
        setMail(ma);
        SendMessage smt=new SendMessage();
        smt.setText(ma.getBody());
        smt.setChatId(String.valueOf(user.getChatid()));
        InlineKeyboardMarkup inKey=new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline=new ArrayList<>();
        List<InlineKeyboardButton> rowInline_1=new ArrayList<>();
        var checkButton=new InlineKeyboardButton();
        checkButton.setText("Проверить на подписку");
        checkButton.setCallbackData(text);
        rowInline_1.add(checkButton);
        rowsInline.add(rowInline_1);
        inKey.setKeyboard(rowsInline);
        smt.setReplyMarkup(inKey);
        try{
            execute(smt);
        }catch(TelegramApiException e){
            System.out.println("We have problem "+e.getMessage());
        }
    }
    public void NoSub(long chatId, String text){
        SendMessage smt=new SendMessage();
        smt.setText("Вы еще не подписались");
        smt.setChatId(String.valueOf(chatId));
        InlineKeyboardMarkup inKey=new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline=new ArrayList<>();
        List<InlineKeyboardButton> rowInline_1=new ArrayList<>();
        var checkButton=new InlineKeyboardButton();
        checkButton.setText("Проверить на подписку");
        checkButton.setCallbackData(text);
        rowInline_1.add(checkButton);
        rowsInline.add(rowInline_1);
        inKey.setKeyboard(rowsInline);
        smt.setReplyMarkup(inKey);
        try{
            execute(smt);
        }catch(TelegramApiException e){
            System.out.println("We have problem "+e.getMessage());
        }
    }
}
