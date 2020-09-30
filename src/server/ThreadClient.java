package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.Objects;

public class ThreadClient extends Thread {
    private ObjectInputStream objectInputStream;   // объявление байтового потока ввода
    private ObjectOutputStream objectOutputStream;   // объявление байтового потока вывода
    private Socket clientSocket;
    private String addres;

    ThreadClient(Socket clientSocket, String addres) {
        this.clientSocket = clientSocket;
        this.addres = addres;
        try {
            objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e1) {
            e1.printStackTrace();
    }
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ScholarshipsAccrual", "root", "");
            Server.addRecordToList(addres + ": Соединение с БД установлено.");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        boolean workClient = true;
        while (workClient) {
            String command = null;
            try {
                command = (String)objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if(!Server.workServer)
                command = "3";
            switch (command) {
                case "1": // Вход_в_систему.Регистрация
                    Server.addRecordToList(addres + ": Вход_в_систему -> Регистрация");
                    break;

                case "2": // Вход_в_систему.Вход
                    Server.addRecordToList(addres + ": Вход_в_систему -> Вход");
                    String admin_user_nobody = null;
                    try {
                        String login = (String) objectInputStream.readObject(),
                                password = (String) objectInputStream.readObject();
                        admin_user_nobody = authorization(connection, login, password);
                        objectOutputStream.writeObject(admin_user_nobody);
                        if(Objects.equals(admin_user_nobody, "nobody"))
                            Server.addRecordToList(addres + ": Ошибка авторизации (без доступа в систему).");
                        else
                            Server.addRecordToList(addres + ": Успешная авторизация. Произведён вход в систему (" + admin_user_nobody + ").");
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    try {
                        if(Objects.equals(admin_user_nobody, "user")) {
                            String students = loadStudentTable(connection);
                            objectOutputStream.writeObject(students);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "3": // Вход_в_систему.Выход
                    workClient = false;
                    Server.addRecordToList(addres + ": Вход_в_систему -> Выход");
                    break;

                case "4": // Регистрация.Зарегистрироваться
                    Server.addRecordToList(addres + ": Регистрация -> Зарегистрироваться");
                    try {
                        String success_fail = registration(connection, (String) objectInputStream.readObject(),
                                (String) objectInputStream.readObject());
                        objectOutputStream.writeObject(success_fail);

                        if(Objects.equals(success_fail, "success"))
                            Server.addRecordToList(addres + ": Успешная регистрация.");
                        else
                            Server.addRecordToList(addres + ": Ошибка регистрации.");
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case "5": // Регистрация -> Назад
                    Server.addRecordToList(addres + ": Регистрация -> Назад");
                    break;

                case "6": // Действи_администратора -> Работа_со_списком_студентов
                    Server.addRecordToList(addres + ": Действи_администратора -> Работа_со_списком_студентов");
                    try {
                        String students = loadStudentTable(connection);
                        objectOutputStream.writeObject(students);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "7": // Действия_администратора -> Настройка_расчёта_стипендий
                    Server.addRecordToList(addres + ": Действия_администратора -> Настройка_расчёта_стипендий");
                    try {
                        String settings[] = loadSettings(connection);
                        objectOutputStream.writeObject(settings);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "8": // Действия_администратора -> Работа_с_БД_пользователей
                    Server.addRecordToList(addres + ": Действия_администратора -> Работа_с_БД_пользователей");
                    try {
                        String users = loadUserTable(connection);
                        objectOutputStream.writeObject(users);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "9": // Действия_администратора -> Назад
                    Server.addRecordToList(addres + ": Действия_администратора -> Назад");
                    break;

                case "10": // Работа_со_списком_студентов.Добавить
                    Server.addRecordToList(addres + ": Работа_со_списком_студентов -> Добавить");
                    try {
                        String student[] = (String[]) objectInputStream.readObject();
                        String success_fail = addStudentInDB(connection, student);
                        if(Objects.equals(success_fail, "success"))
                            Server.addRecordToList(addres + ": Студент успешно добавлен.");
                        else
                            Server.addRecordToList(addres + ": Ошибка добавления студента.");
                        objectOutputStream.writeObject(success_fail);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case "11": // Работа_со_списком_студентов.Редактировать
                    Server.addRecordToList(addres + ": Работа_со_списком_студентов -> Редактировать");
                    try {
                        String student[] = (String[]) objectInputStream.readObject();
                        String success_fail = editStudentInDB(connection, student);
                        if(Objects.equals(success_fail, "success"))
                            Server.addRecordToList(addres + ": Студент успешно добавлен.");
                        else
                            Server.addRecordToList(addres + ": Ошибка добавления студента.");
                        objectOutputStream.writeObject(success_fail);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case "12": // Работа_со_списком_студентов.Удалить
                    Server.addRecordToList(addres + ": Работа_со_списком_студентов -> Удалить");
                    try {
                        Long id_student = (Long) objectInputStream.readObject();
                        String success_fail = deleteStudentInDB(connection, id_student);
                        if(Objects.equals(success_fail, "success"))
                            Server.addRecordToList(addres + ": Студент успешно удалён.");
                        else
                            Server.addRecordToList(addres + ": Ошибка удаления студента.");
                        objectOutputStream.writeObject(success_fail);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case "13": // Работа_со_списком_студентов.Формировать_отчёт
                    Server.addRecordToList(addres + ": Работа_со_списком_студентов -> Формировать_отчёт");
                    break;

                case "14": // Работа_со_списком_студентов.Фильтровать
                    Server.addRecordToList(addres + ": Работа_со_списком_студентов -> Фильтровать");
                    try {
                        String students = loadStudentTable(connection);
                        objectOutputStream.writeObject(students);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "16": // Работа_со_списком_студентов.Назад
                    Server.addRecordToList(addres + ": Работа_со_списком_студентов -> Назад");
                    break;

                case "17": // Настройка_расчёта_стипендий.Сохранить
                    Server.addRecordToList(addres + ": Настройка_расчёта_стипендий -> Сохранить");
                    try {
                        String settings[] = (String[]) objectInputStream.readObject();
                        String success_fail = resetSettings(connection, settings);
                        objectOutputStream.writeObject(success_fail);
                        if(Objects.equals(success_fail, "success"))
                            Server.addRecordToList(addres + ": Успешное изменение настроек.");
                        else
                            Server.addRecordToList(addres + ": Ошибка изменения настроек.");
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case "18": // Настройка_расчёта_стипендий.Назад
                    Server.addRecordToList(addres + ": Настройка_расчёта_стипендий -> Назад");
                    break;

                case "19": // Работа_с_БД_пользователей.Добавить
                    Server.addRecordToList(addres + ": Работа_с_БД_пользователей -> Добавить");
                    try {
                        String user[] = (String[]) objectInputStream.readObject();
                        String success_fail = addUserInDB(connection, user);
                        if(Objects.equals(success_fail, "success"))
                            Server.addRecordToList(addres + ": Пользователь успешно добавлен.");
                        else
                            Server.addRecordToList(addres + ": Ошибка добавления пользователя.");
                        objectOutputStream.writeObject(success_fail);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case "20": // Работа_с_БД_пользователей.Редактировать
                    Server.addRecordToList(addres + ": Работа_с_БД_пользователей -> Редактировать");
                    try {
                        String user[] = (String[]) objectInputStream.readObject();
                        String success_fail = editUserInDB(connection, user);
                        if(Objects.equals(success_fail, "success"))
                            Server.addRecordToList(addres + ": Пользователь успешно редактирован.");
                        else
                            Server.addRecordToList(addres + ": Ошибка редактирования пользователя.");
                        objectOutputStream.writeObject(success_fail);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case "21": // Работа_с_БД_пользователей.Удалить
                    Server.addRecordToList(addres + ": Работа_с_БД_пользователей -> Удалить");
                    try {
                        Long id_user = (Long) objectInputStream.readObject();
                        String success_fail = deleteUserInDB(connection, id_user);
                        if(Objects.equals(success_fail, "success"))
                            Server.addRecordToList(addres + ": Пользователь успешно удалён.");
                        else
                            Server.addRecordToList(addres + ": Ошибка удаления пользователя.");
                        objectOutputStream.writeObject(success_fail);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case "22": // Работа_с_БД_пользователей.Назад
                    Server.addRecordToList(addres + ": Работа_с_БД_пользователей -> Назад");
                    break;

                case "23": // Работа_со_списком_студентов.Обновить
                    Server.addRecordToList(addres + ": Работа_со_списком_студентов -> Обновить");
                    try {
                        String students = loadStudentTable(connection);
                        objectOutputStream.writeObject(students);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "24": // Работа_с_БД_пользователей.Обновить
                    Server.addRecordToList(addres + ": Работа_с_БД_пользователей -> Обновить");
                    try {
                        String users = loadUserTable(connection);
                        objectOutputStream.writeObject(users);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case "25": // Запрос_настроек_расчёта_стипендий
                    Server.addRecordToList(addres + ": Запрос настроек расчёта стипендий");
                    try {
                        String settings[] = loadSettings(connection);
                        objectOutputStream.writeObject(settings);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                default: Server.addRecordToList(addres + ": Неизвестная команда");
            }
        }
        try {
            clientSocket.close();   // закрытие сокета, выделенного для клиента
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
            if (objectInputStream != null) {
                objectInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.interrupt();
        }
    }

    private String authorization(Connection connection, String login, String password) {
        String admin_user_nobody = "nobody";
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM users");
            while(resultSet.next())
                if (Objects.equals(resultSet.getString("login"), login) && Objects.equals(resultSet.getString("password"), password))
                    if (Objects.equals(resultSet.getString("role"), "1"))
                        admin_user_nobody = "admin";
                    else
                        admin_user_nobody = "user";
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return admin_user_nobody;
    }

    private String registration(Connection connection, String login, String password) {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM users");
            while(resultSet.next())
                if (Objects.equals(resultSet.getString("login"), login))
                    return "fail";
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement
                ("INSERT INTO users (login, password, role) VALUES (?, ?, ?)")) {
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, "0");
            preparedStatement.executeUpdate();
            preparedStatement.close();
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    private String loadStudentTable(Connection connection) {
        StringBuilder student = new StringBuilder();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT increases.id_student, fio, average_score, scholarship, " +
                    "scientific_work, cultural_activities, social_help, payment_for_trade_union_committee, payment_for_the_BRYU, " +
                    "payment_for_a_hostel, amount_of_fines FROM scholarships, increases, deductions WHERE " +
                    "(increases.id_student=deductions.id_student && deductions.id_student=scholarships.id_student)");
            while(resultSet.next()) {
                student.append(resultSet.getString("id_student")).append("#");
                student.append(resultSet.getString("fio")).append("#");
                student.append(resultSet.getString("average_score")).append("#");
                student.append(resultSet.getString("scientific_work")).append("#");
                student.append(resultSet.getString("cultural_activities")).append("#");
                student.append(resultSet.getString("social_help")).append("#");
                student.append(resultSet.getString("payment_for_trade_union_committee")).append("#");
                student.append(resultSet.getString("payment_for_the_BRYU")).append("#");
                student.append(resultSet.getString("payment_for_a_hostel")).append("#");
                student.append(resultSet.getString("amount_of_fines")).append("#");

                student.append(resultSet.getString("scholarship")).append("#");
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return student.toString();
    }

    private String loadUserTable(Connection connection) {
        StringBuilder users = new StringBuilder();

        try {
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT * FROM users ");
            while(resultSet.next()) {
                users.append(resultSet.getString("id_user")).append("#");
                users.append(resultSet.getString("login")).append("#");
                users.append(resultSet.getString("password")).append("#");
                users.append(resultSet.getString("role")).append("#");
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users.toString();
    }

    private String addStudentInDB(Connection connection, String[] student) {
        try{
            connection.setAutoCommit(false);                                // начало транзакции
            PreparedStatement preparedStatement = connection.prepareStatement
                    ("INSERT INTO scholarships (fio, average_score, scholarship) VALUES (?, ?, ?)");
            preparedStatement.setString(1, student[0]);
            preparedStatement.setString(2, student[1]);
            preparedStatement.setString(3, student[2]);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement
                    ("INSERT INTO increases (scientific_work, cultural_activities, social_help) VALUES (?, ?, ?)");
            preparedStatement.setString(1, student[3]);
            preparedStatement.setString(2, student[4]);
            preparedStatement.setString(3, student[5]);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement
                    ("INSERT INTO deductions (payment_for_trade_union_committee, payment_for_the_BRYU, payment_for_a_hostel, amount_of_fines) VALUES (?, ?, ?, ?)");
            preparedStatement.setString(1, student[6]);
            preparedStatement.setString(2, student[7]);
            preparedStatement.setString(3, student[8]);
            preparedStatement.setString(4, student[9]);
            preparedStatement.executeUpdate();

            connection.commit();                                            // завершение транзакции
            preparedStatement.close();
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    private String editStudentInDB(Connection connection, String[] student) {
        try {
            connection.setAutoCommit(false);                                // начало транзакции
            PreparedStatement preparedStatement = connection.prepareStatement
                    ("UPDATE scholarships SET fio=?, average_score=?, scholarship=? WHERE id_student=?");
            preparedStatement.setString(1, student[1]);
            preparedStatement.setString(2, student[2]);
            preparedStatement.setString(3, student[3]);
            preparedStatement.setString(4, student[0]);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement
                    ("UPDATE increases SET scientific_work=?, cultural_activities=?, social_help=? WHERE id_student=?");
            preparedStatement.setString(1, student[4]);
            preparedStatement.setString(2, student[5]);
            preparedStatement.setString(3, student[6]);
            preparedStatement.setString(4, student[0]);
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement
                    ("UPDATE deductions SET payment_for_trade_union_committee=?, payment_for_the_BRYU=?, payment_for_a_hostel=?, amount_of_fines=? WHERE id_student=?");
            preparedStatement.setString(1, student[7]);
            preparedStatement.setString(2, student[8]);
            preparedStatement.setString(3, student[9]);
            preparedStatement.setString(4, student[10]);
            preparedStatement.setString(5, student[0]);
            preparedStatement.executeUpdate();

            connection.commit();                                            // завершение транзакции
            preparedStatement.close();
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    private String addUserInDB(Connection connection, String[] user) {
        try{
            PreparedStatement preparedStatement = connection.prepareStatement
                    ("INSERT INTO users (login, password, role) VALUES (?, ?, ?)");
            preparedStatement.setString(1, user[0]);
            preparedStatement.setString(2, user[1]);
            preparedStatement.setString(3, user[2]);
            preparedStatement.executeUpdate();

            preparedStatement.close();
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    private String editUserInDB(Connection connection, String[] user) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement
                    ("UPDATE users SET login=?, password=?, role=? WHERE id_user=?");
            preparedStatement.setString(1, user[1]);
            preparedStatement.setString(2, user[2]);
            preparedStatement.setString(3, user[3]);
            preparedStatement.setString(4, user[0]);
            preparedStatement.executeUpdate();

            preparedStatement.close();
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    private String deleteStudentInDB(Connection connection, Long id_student) {
        try {
            connection.setAutoCommit(false);                                // начало транзакции
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM scholarships WHERE id_student=?");
            preparedStatement.setString(1, String.valueOf(id_student));
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement("DELETE FROM increases WHERE id_student=?");
            preparedStatement.setString(1, String.valueOf(id_student));
            preparedStatement.executeUpdate();

            preparedStatement = connection.prepareStatement("DELETE FROM deductions WHERE id_student=?");
            preparedStatement.setString(1, String.valueOf(id_student));
            preparedStatement.executeUpdate();

            connection.commit();                                            // завершение транзакции
            preparedStatement.close();
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    private String deleteUserInDB(Connection connection, Long id_user) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM users WHERE id_user=?");
            preparedStatement.setString(1, String.valueOf(id_user));
            preparedStatement.executeUpdate();

            preparedStatement.close();
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }

    private String[] loadSettings(Connection connection) {
        String settings[] = new String[9];

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM settings ");
            while(resultSet.next()) {
                settings[0] = resultSet.getString("basic_scholarship");
                settings[1] = resultSet.getString("fellowship_multiplier_1");
                settings[2] = resultSet.getString("fellowship_multiplier_2");
                settings[3] = resultSet.getString("fellowship_multiplier_3");
                settings[4] = resultSet.getString("fellowship_multiplier_4");
                settings[5] = resultSet.getString("multiplier_for_scientific_work");
                settings[6] = resultSet.getString("multiplier_for_cultural_activities");
                settings[7] = resultSet.getString("payment_for_trade_union_committee");
                settings[8] = resultSet.getString("payment_for_the_BRYU");
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settings;
    }

    private String resetSettings(Connection connection, String[] settings) {
        try (PreparedStatement preparedStatement = connection.prepareStatement
                ("UPDATE settings SET basic_scholarship=?, fellowship_multiplier_1=?, fellowship_multiplier_2=?, fellowship_multiplier_3=?, fellowship_multiplier_4=?, multiplier_for_scientific_work=?, multiplier_for_cultural_activities=?, payment_for_trade_union_committee=?, payment_for_the_BRYU=? WHERE id_setting=1")) {
            preparedStatement.setString(1, settings[0]);
            preparedStatement.setString(2, settings[1]);
            preparedStatement.setString(3, settings[2]);
            preparedStatement.setString(4, settings[3]);
            preparedStatement.setString(5, settings[4]);
            preparedStatement.setString(6, settings[5]);
            preparedStatement.setString(7, settings[6]);
            preparedStatement.setString(8, settings[7]);
            preparedStatement.setString(9, settings[8]);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            return "success";
        } catch (SQLException e) {
            e.printStackTrace();
            return "fail";
        }
    }
}
