import java.sql.*;
import java.util.*;
import java.io.*;

public class BooksLibrary {
    private static final String dbClassName = "com.mysql.jdbc.Driver";

    private static final String dbName = "blib";
    private static final String CONNECTION = "jdbc:mysql://127.0.0.1/" + dbName + "?useUnicode=true&characterEncoding=utf8&charSet=utf8&" +
            "sessionVariables=character_set_server=utf8&sessionVariables=collation_server=utf8_general_ci";

    private static final String user = "root";
    private static final String password = "root_password";
    private static final String INP = "blib>";
    private static final String HELP = "- Чтобы внести новую книгу в базу, напишите: \n" +
            "    Добавить [Имя_автора] [Фамилия_автора] [Название книги] [Расположение]\n" +
            "- Чтобы узнать расположение книги, введите, заполняя неизвестные вам характеристики вопросом:\n" +
            "    Найти [Имя_автора] [Фамилия_автора] [Название книги] \n" +
            "- Чтобы завершить использование программой, напишите:\n" +
            "   \"ВЫЙТИ\" или \"FINISH\"\n" +
            "- Чтобы повторить последний запрос, напишите \"last\" или \"посл\"\n" +
            "На заметку: К сожалению, по неизвестным причинам при абсолютно корректном вводе может \n" +
            "выдать ошибку. В таком случае просто попробуйте еще раз. Перед запросом можете нажать Enter с пустой(лучше не пустой строкой).\n" +
            INP;

    private static Scanner in = new Scanner(System.in);
    private static PrintWriter out = new PrintWriter(System.out);
    private static String[][] table;
    // JDBC variables for opening and managing connection
    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;
    private static int COUNT;
    private static String input;

    public static void main(String[] args) throws
            ClassNotFoundException, SQLException {
        Class.forName(dbClassName);
        Properties p = new Properties();
        p.put("user", user);
        p.put("password", password);
        COUNT = count(p);
        boolean correct = false, doneThings = false, inputIsLast = false;
        String query;
        fillTable(p);
        out.print("Чтобы узнать возможные команды, напишите ПОМОЩЬ или HELP.\n" +
                INP);
        out.flush();
        String last = "";
        while (true) {
            correct = false;
            inputIsLast = false;
            input = in.nextLine();
            input = input.trim();
            input = input.replace("�", "");
            input = input.replace("ё", "е");
            out.print("----UR INPUT IS:" + input + " -----\n");
            if (input.toLowerCase().equals("last") || input.toLowerCase().equals("посл")) {
                input = last;
                inputIsLast = true;
            }
            if (input.toLowerCase().equals("help") || input.toLowerCase().equals("помощь")) {
                out.print(HELP);
                correct = true;
            }
            if (input.toLowerCase().equals("выйти") || input.toLowerCase().equals("finish")) {
                break;
            }
            String[] split = input.split(" ");
            if (split[0].toLowerCase().equals("добавить") && split.length > 4) {

                if (split.length > 4) {//Собирание составного названия в одну строку
                    String temporary = split[split.length - 1];
                    for (int i = 4; i < split.length - 1; i++) {
                        split[3] = split[3].concat(" " + split[i]);
                    }
                    split[split.length - 1] = temporary;
                }

                doneThings = true;
                correct = true;
                query = "INSERT INTO books VALUE ('" + split[1] + "', '" + split[2] + "', '" + split[3] + "', '" + split[split.length - 1] + "', NULL);";
                add(query, p);
//                out.println(query);
            } else if (split[0].toLowerCase().equals("найти") && split.length > 3) {//TODO: поиск по части названия

                if (split.length > 4) {//Собирание составного названия в одну строку
                    for (int i = 4; i < split.length; i++) {//todo: Удалять из строк символ �
                        split[3] = split[3].concat(" " + split[i]);
                    }
                }

                doneThings = true;
                correct = true;//TODO: Добавить систему проверки правильности написанного запроса
                boolean hasStarted = false;
                query = "SELECT first_name, last_name,title, location FROM books WHERE ";
                if (!split[1].equals("?")) {
                    query = query.concat("first_name=\"" + split[1] + "\"");
                    hasStarted = true;
                }
                if (!split[2].equals("?")) {
                    if (hasStarted) query = query.concat(" AND ");
                    else hasStarted = true;
                    query = query.concat("last_name=\"" + split[2] + "\"");
                }
                if (!split[3].equals("?")) {
                    if (hasStarted) query = query.concat(" AND ");
                    else hasStarted = true;
                    query = query.concat("title=\"" + split[3]) + "\"";
                }
                query = query.concat(";");
                if (hasStarted) getLocation(query, p);
                else out.print("Некорректный ввод. Убедитесь, что все написано по следующему шаблону:\n" +
                        "    Найти [Имя_автора] [Фамилия_автора] [Название книги]");
            }
            if (count(p) > COUNT) out.print("Успешно добавлено\n" + INP);
            if (!correct && !inputIsLast) out.print("Некорректный ввод. Попробуйте еще раз.\n" + INP);
            if (!inputIsLast) last = input;
            if (correct) COUNT = count(p);
            out.flush();
        }


        if (!doneThings) out.println("GOODBYE, MASTER!");
        else out.println("WELL DONE, MASTER! GOODBYE!");
        out.flush();
        in.close();
        out.close();
    }

    private static void add(String query, Properties p) {
        try {

            con = DriverManager.getConnection(CONNECTION, p);
            stmt = con.createStatement();
            stmt.executeUpdate(query);
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close connection ,stmt and result20\
            // set here
            try {
                con.close();
            } catch (SQLException se) { /*can't do anything */ }
            try {
                stmt.close();
            } catch (SQLException se) { /*can't do anything */ }
//            out.print("Успешно добавлено." + INP);//TODO: Добавить условие
        }
    }

    private static void getLocation(String query, Properties p) {//выводит месторасположение книг, соответствующих запросу
        try {

            con = DriverManager.getConnection(CONNECTION, p);
            stmt = con.createStatement();
            rs = stmt.executeQuery(query);
            boolean found = false;
            while (rs.next()) {
                String first_name = rs.getString(1);
                String last_name = rs.getString(2);
                String title = rs.getString(3);
                String location = rs.getString(4);
//                int id = rs.getInt(5);
                out.print("Книга \"" + first_name + " " + last_name + " - " + title + "\" расположена в секции " + location + "\n" + INP);
                found = true;
            }
            if (!found) out.print("К сожалению, не найдено ничего. Попробуйте еще раз." + "\n" + INP);
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close connection ,stmt and resultset here
            try {
                con.close();
            } catch (SQLException se) { /*can't do anything */ }
            try {
                stmt.close();
            } catch (SQLException se) { /*can't do anything */ }
            try {
                rs.close();
            } catch (SQLException se) { /*can't do anything */ }
        }
    }

    private static void fillTable(Properties p) {//создает таблицу
        try {

            String query = "SELECT * FROM books;";
            con = DriverManager.getConnection(CONNECTION, p);
            stmt = con.createStatement();
            table = new String[COUNT + 1][5];//5 или нет?
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                String first_name = rs.getString(1);
                String last_name = rs.getString(2);
                StringBuilder author = new StringBuilder();

                //составляю строку, отвечающую за автора
                if (first_name.contains(",") && last_name.contains(",")) {//если два автора(для большего кол-ва не работает)
                    String[] aSplit = first_name.split(",");
                    String[] bSplit = last_name.split(",");
                    if (aSplit.length == bSplit.length) {
                        author.append(shorten(aSplit[0])).append(" ").append(bSplit[0]).append(", ").append(shorten(aSplit[1])).append(" ").append(bSplit[1]);
                    }
                } else if (last_name.equals("-")) {
                    author.append(first_name);
                } else {
                    author.append(shorten(first_name)).append(" ").append(last_name);
                }

                String title = rs.getString(3);
                String location = rs.getString(4);
                int id = rs.getInt(5);
                table[id][0] = author.toString();//заполнение таблицы
                table[id][1] = title;
                table[id][2] = location;
                table[id][3] = first_name;
                table[id][4] = last_name;
//                int id = rs.getInt(5);
//                out.print(author.toString()+ " - " + title + ", " + location + " " + "\n");
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close connection ,stmt and resultset here
            try {
                con.close();
            } catch (SQLException se) { /*can't do anything */ }
            try {
                stmt.close();
            } catch (SQLException se) { /*can't do anything */ }
            try {
                rs.close();
            } catch (SQLException se) { /*can't do anything */ }
        }
    }

    private static int count(Properties p) {//возвращает количество строк в таблице
        try {

            String query = "SELECT COUNT(*) FROM books;";
            con = DriverManager.getConnection(CONNECTION, p);
            stmt = con.createStatement();

            rs = stmt.executeQuery(query);
            while (rs.next()) {
                int count = rs.getInt(1);
                return count;
            }
        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            //close connection ,stmt and resultset here
            try {
                con.close();
            } catch (SQLException se) {
            }
            try {
                stmt.close();
            } catch (SQLException se) {
            }
            try {
                rs.close();
            } catch (SQLException se) {
            }
        }
        return -1;
    }

    private static String shorten(String name) {//инициалы из имени
        if (name.contains(".") || name.equals("-")) {
            return name;
        } else {
            return (name.charAt(0) + ".");
        }
    }
//TODO: Добавить такие функции как: показать таблицу всех или с таким-то автором, на такой-то полке и т.п.
    //TODO:Улучшить функцию поиска обяз-но. Примеры: Марк Твен = М. Твен, Д. Букин = Денис Букин
    //если имеет точку в имени, то искать по author, если нет, то отдельно имя и фамилию
}
