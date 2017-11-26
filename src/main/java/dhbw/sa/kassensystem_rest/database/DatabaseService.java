package dhbw.sa.kassensystem_rest.database;

import dhbw.sa.kassensystem_rest.database.entity.Item;
import dhbw.sa.kassensystem_rest.database.entity.Itemdelivery;
import dhbw.sa.kassensystem_rest.database.entity.Order;
import dhbw.sa.kassensystem_rest.database.entity.Table;
import dhbw.sa.kassensystem_rest.exceptions.DataException;
import dhbw.sa.kassensystem_rest.exceptions.MySQLServerConnectionException;
import dhbw.sa.kassensystem_rest.printer.PrinterService;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Null;
import java.sql.*;
import java.util.ArrayList;

/**
 * {@inheritDoc}
 *
 * Implementierung des DatabaseService_Interfaces
 *
 * @author Marvin Mai
 */

@Service
public class DatabaseService implements DatabaseService_Interface{
    /*
     *  items Alle in der Datenbank befindlichen Items werden hier gespeichert.
     *  tables Alle in der Datenbank befindlichen Tables werden hier gespeichert.
     *  orders Alle in der Datenbank befindlichen Orders werden hier gespeichert.
     *  itemdeliveries Alle in der Datenbank befindlichen itemdeliveries werden hier gespeichert.
     *  connection Eine Instanz einer Verbindung zur Datenbank.
     *  dbp Beinhaltet eine Beschreibung der Daten die zur Verbindung zur Datenbank noetig sind.
     */

    private final DatabaseProperties dbp = new DatabaseProperties();

    private Connection connection = null;

    private ArrayList<Item> items = new ArrayList<>();
    private ArrayList<Table> tables = new ArrayList<>();
    private ArrayList<Order> orders = new ArrayList<>();
    private ArrayList<Itemdelivery> itemdeliveries = new ArrayList<>();
    private ArrayList<Item> itemsWithoutQuantity = new ArrayList<>();

    public DatabaseService() {
        this.connect();
    }

    @Override
    public void connect() {
        logInf("Connecting database...");

        try{
            connection = DriverManager.getConnection(dbp.getUrl(), dbp.getUsername(), dbp.getPassword());
            logInf("Database connected!");
        }catch(SQLException e) {
            logErr("Verbindung zur Datenbank fehlgeschlagen!");
            throw new IllegalStateException("Verbindung zur Datenbank fehlgeschlagen!", e);
        }
    }
    @Override
    public void disconnect() {
        if(connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("\nDatabase disconnected!");
            } catch(SQLException e)  { e.printStackTrace();}
        }
    }

    //Getting Table-Data from the database
    @Override
    public ArrayList<Item> getAllItems() throws MySQLServerConnectionException {
        this.items.clear();

        logInf("Getting Items from MySQL-Database.");

        this.itemdeliveries = this.getAllItemdeliveries();
        this.orders = this.getAllOrders();
        this.itemsWithoutQuantity = this.getAllItemsEmpty();

        try {
            String query = "SELECT itemID, name, retailprice, available " +
                    "FROM " + dbp.getDatabase() + ".items";
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            while(rs.next()) {
                //get each Item from DB
                int itemID = rs.getInt("itemID");
                String name = rs.getString("name");
                double retailprice = rs.getFloat("retailprice");
                retailprice = round(retailprice);

                int quantity = getItemQuantity(itemID);

                boolean available = rs.getBoolean("available");
                items.add(new Item(itemID, name, retailprice, quantity, available));
            }
            return this.items;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLServerConnectionException();
        }
    }
    @Override
    public ArrayList<Table> getAllTables() throws MySQLServerConnectionException {
        this.tables.clear();

        logInf("Getting Tables from MySQL-Database.");

        try {
            String query = "SELECT tableID, name, available " +
                    "FROM " + dbp.getDatabase() + ".tables";
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            while(rs.next()) {
                //get each table from DB
                int tableID = rs.getInt("tableID");
                String name = rs.getString("name");
                boolean available = rs.getBoolean("available");
                tables.add(new Table(tableID, name, available));
            }
            return this.tables;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLServerConnectionException();
        }
    }
    @Override
    public ArrayList<Order> getAllOrders() throws MySQLServerConnectionException  {
        this.orders.clear();

        logInf("Getting Orders from MySQL-Database.");

        try {
            String query = "SELECT orderID, itemIDs, price, date, tableID, paid " +
                    "FROM " + dbp.getDatabase() + ".orders";
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            while(rs.next()) {
                int orderID = rs.getInt("orderID");
                String itemIdString = rs.getString("itemIDs");
                int tableID = rs.getInt("tableID");
                double price = rs.getFloat("price");
                price = round(price);
                DateTime dateTime = convertSqlTimestampToJodaDateTime(rs.getTimestamp("date"));
                boolean paid = rs.getBoolean("paid");

                this.orders.add(new Order(orderID, itemIdString, tableID, price, dateTime, paid));
            }
            return this.orders;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLServerConnectionException();
        }
    }
    @Override
    public ArrayList<Itemdelivery> getAllItemdeliveries() throws MySQLServerConnectionException {
        this.itemdeliveries.clear();

        logInf("Getting Itemdeliveries from MySQL-Database.");

        try {
            String query = "SELECT itemdeliveryID, itemID, quantity " +
                    "FROM " + dbp.getDatabase() + ".itemdeliveries";
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            while(rs.next()) {
                //get each Itemdelivery from DB
                int itemdeliveryID = rs.getInt("itemdeliveryID");
                int itemID = rs.getInt("itemID");
                int quantity = rs.getInt("quantity");
                itemdeliveries.add(new Itemdelivery(itemdeliveryID, itemID, quantity));
            }
            return this.itemdeliveries;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLServerConnectionException();
        }
    }

    //Datenbankinhalte mit Angabe der ID erhalten
    public Order getOrderById(int orderID) {

        if(orderID == 0) {
            logErr("Order-ID may not be null.");
            throw new NullPointerException("No Order-ID given.");
        }

        logInf("Getting Order with ID " + orderID + ".");

        for(Order o: this.getAllOrders()) {
            if(o.getOrderID() == orderID)
                return o;
        }

        logErr("Order with ID " + orderID + " doesn't exist in the database.");
        throw new NullPointerException("Order-ID " + orderID + " not found.");
    }

    public Item getItemById(int itemID) {

        if(itemID == 0) {
            logErr("Item-ID may not be null.");
            throw new NullPointerException("No Item-ID given.");
        }

        logInf("Getting Item with ID " + itemID + ".");

        for(Item i: this.getAllItems()) {
            if(i.getItemID() == itemID)
                return i;
        }

        logErr("Item with ID " + itemID + " doesn't exist in the database.");
        throw new NullPointerException("Item-ID " + itemID + " not found.");
    }

    public Table getTableById(int tableID) {

        if(tableID == 0) {
            logErr("Table-ID may not be null.");
            throw new NullPointerException("No Table-ID given.");
        }

        logInf("Getting Table with ID " + tableID + ".");

        for(Table t: this.getAllTables()) {
            if(t.getTableID() == tableID)
                return t;
        }

        logErr("Table with ID " + tableID + " doesn't exist in the database.");
        throw new NullPointerException("Table-ID " + tableID + " not found.");
    }

    public Itemdelivery getItemdeliveryById(int itemdeliveryID) {

        if(itemdeliveryID == 0) {
            logErr("Itemdelivery-ID may not be null.");
            throw new NullPointerException("No Itemdelivery-ID given.");
        }

        logInf("Getting Itemdelivery with ID " + itemdeliveryID + ".");

        for(Itemdelivery i: this.getAllItemdeliveries()) {
            if(i.getItemdeliveryID() == itemdeliveryID)
                return i;
        }

        logErr("Itemdelivery with ID " + itemdeliveryID + " doesn't exist in the database.");
        throw new NullPointerException("Itemdelivery-ID " + itemdeliveryID + " not found.");

    }

    //Adding data to the database
    @Override
    public void addItem(Item item) throws MySQLServerConnectionException {

        logInf("Adding Item to MySQL-Database.");

        try {
            String query =  "INSERT INTO " + dbp.getDatabase() + ".items(itemID, name, retailprice, available) " +
                            "VALUES(DEFAULT, ?, ?, ?)";
            PreparedStatement pst = connection.prepareStatement(query);

            pst.setString(1, item.getName());
            pst.setDouble(2, item.getRetailprice());
            pst.setBoolean(3, item.isAvailable());
            pst.executeUpdate();

            //ID des neu erzeugten Items ermitteln, um anschließend hierfuer einen neuen Wareneingang anzulegen
            query = "SELECT * FROM " + dbp.getDatabase() + ".items ORDER BY itemID DESC LIMIT 1";
            pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            int itemID = 0;
            while(rs.next()) {
                itemID = rs.getInt("itemID");
            }
            Itemdelivery itemdelivery = new Itemdelivery(itemID, item.getQuantity());
            addItemdelivery(itemdelivery);
        } catch(SQLException e) {
            e.printStackTrace();
            throw new MySQLServerConnectionException();
        }
    }
    @Override
    public void addTable(Table table) throws MySQLServerConnectionException {

        logInf("Adding Table to MySQL-Database.");

        try {
            String query =  "INSERT INTO " + dbp.getDatabase() + ".tables(tableID, name, available) " +
                            "VALUES(DEFAULT, ?, ?)";
            PreparedStatement pst = connection.prepareStatement(query);

            pst.setString(1, table.getName());
            pst.setBoolean(2, table.isAvailable());
            pst.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
            throw new MySQLServerConnectionException();
        }
    }
    @Override
    public void addOrder(Order order) throws MySQLServerConnectionException, DataException {

        logInf("Adding Order to MySQL-Database.");

        //Vollständigkeit der Order ueberpruefen
        if(order.getItems() == null
                || order.getOrderID() != 0
                || order.getTable() == 0
                || order.getPrice() == 0
                || order.getDate() == null)
            throw new DataException("Die Order ist unvollständig!");

        if(order.isPaid()) {
            printOrder(order, true);
            printOrder(order, false);
        }
        else
            printOrder(order, true);

        try {
            String query =  "INSERT INTO " + dbp.getDatabase() + ".orders(orderID, itemIDs, price, date, tableID, paid)" +
                            "VALUES(DEFAULT, ?, ?, ?, ?, ?)";
            PreparedStatement pst = connection.prepareStatement(query);

            pst.setString(1, order.getItems());
            pst.setDouble(2, order.getPrice());
            pst.setObject(3, convertJodaDateTimeToSqlTimestamp(order.getDate()) );
            pst.setInt(4, order.getTable());
            pst.setBoolean(5, order.isPaid());
            pst.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
            throw new MySQLServerConnectionException();
        }
    }
    @Override
    public void addItemdelivery(Itemdelivery itemdelivery) throws MySQLServerConnectionException {

        logInf("Adding Itemdelivery to MySQL-Database.");

        try {
            String query =  "INSERT INTO " + dbp.getDatabase() + ".itemdeliveries(itemdeliveryID, itemID, quantity) " +
                    "VALUES(DEFAULT, ?, ?)";
            PreparedStatement pst = connection.prepareStatement(query);

            pst.setInt(1, itemdelivery.getItemID());
            pst.setInt(2, itemdelivery.getQuantity());
            pst.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
            throw new MySQLServerConnectionException();
        }
    }

    //Updating data in database
    @Override
    public void updateItem(int itemID, Item item) {

        logInf("Updating Item with ID " + itemID + ".");

        try {
            String query =  "UPDATE " + dbp.getDatabase() + ".items " +
                            "SET name = ?, retailprice = ?, available = ? " +
                            "WHERE itemID = " + itemID;
            PreparedStatement pst = connection.prepareStatement(query);

            pst.setString(1, item.getName());
            pst.setDouble(2, item.getRetailprice());
            pst.setBoolean(3, item.isAvailable());
            pst.executeUpdate();
        } catch(SQLException e) { e.printStackTrace(); }
    }
    @Override
    public void updateTable(int tableID, Table table) {

        logInf("Updating Table with ID " + tableID + ".");

        try {
            String query =  "UPDATE " + dbp.getDatabase() + ".tables " +
                            "SET name = ?, available = ? " +
                            "WHERE tableID = " + tableID;
            PreparedStatement pst = connection.prepareStatement(query);

            pst.setString(1, table.getName());
            pst.setBoolean(2, table.isAvailable());
            pst.executeUpdate();
        } catch(SQLException e) { e.printStackTrace(); }
    }
    @Override
    public void updateOrder(int orderID, Order order) {

        logInf("Updating Order with ID " + orderID + ".");

        //Kontrollieren ob ID existiert
        if(this.getOrderById(orderID) == null)
            throw new DataException("Bestellung mit der ID " + orderID + " existiert nicht!");

        //Vollständigkeit der Order ueberpruefen
        if(order.getItems() == null
                || order.getOrderID() != 0
                || order.getTable() == 0
                || order.getPrice() == 0
                || order.getDate() == null)
            throw new DataException("Die Order ist unvollständig!");

        if(order.isPaid())
            //Kundenbeleg ausdrucken, wenn bezahlt wird
            printOrder(order, false);

        //Bei jedem Update die Differenz zur bisherigen Bestellung erkennen und in einem Kuechenbeleg ausdrucken
        Order oldOrder = this.getOrderById(orderID);
        Order newOrder = order;
        Order diffOrder = getDiffOrder(oldOrder, newOrder);

        printOrder(diffOrder, true);

        try {
            String query =  "UPDATE " + dbp.getDatabase() + ".orders " +
                            "SET itemIDs = ?, price = ?, date = ?, tableID = ?, paid = ? " +
                            "WHERE orderID = " + orderID;
            PreparedStatement pst = connection.prepareStatement(query);

            pst.setString(1, order.getItems());
            pst.setDouble(2, order.getPrice());
            pst.setObject(3, convertJodaDateTimeToSqlTimestamp(order.getDate()) );
            pst.setInt(4, order.getTable());
            pst.setBoolean(5, order.isPaid());
            pst.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();

        }
    }
    //Deleting data from database
    /*
      Beim Loeschen eines Items oder Tables wird dieser nur als nicht verfuegbar markiert, verbleiben aber in der Datenbank.
      Somit ist sichergestellt, dass fuer bisherige Orders alle Daten verfuegbar bleiben.
     */
    @Override
    public void deleteItem(int itemID) {

        logInf("Setting Item with ID " + itemID + " to unavailable.");

        Item item = this.getItemById(itemID);

        item.setAvailable(false);

        this.updateItem(itemID, item);
    }
    @Override
    public void deleteTable(int tableID) {

        logInf("Setting Table with ID " + tableID + " to unavailable.");

        Table table = this.getTableById(tableID);

        table.setAvailable(false);

        this.updateTable(tableID, table);
    }
    @Override
    public void deleteOrder(int orderID) {

        logInf("Deleting Order with ID " + orderID + ".");

        /*
          Loeschen einer Order loescht diese unwiederruflich aus der Datenbank.
         */
        try {
            String query =  "DELETE FROM " + dbp.getDatabase() + ".orders " +
                    "WHERE orderID = " + orderID;
            PreparedStatement pst = connection.prepareStatement(query);

            pst.executeUpdate();
        } catch(SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void deleteItemdelivery(int itemdeliveryID) {

        logInf("Deleting Itemdelivery with ID " + itemdeliveryID + ".");

        try {
            String query =  "DELETE FROM " + dbp.getDatabase() + ".itemdeliveries " +
                    "WHERE itemdeliveryID = " + itemdeliveryID;
            PreparedStatement pst = connection.prepareStatement(query);

            pst.executeUpdate();
        } catch(SQLException e) { e.printStackTrace(); }
    }


    //Drucken einer Order

    public void printOrderById(int orderID) {
        this.printOrder(getOrderById(orderID), false);
    }

    /**
     * Druckt eine Order aus.
     * @param order die auszudruckende Order.
     * @param kitchenReceipt sagt dem PrinterService, ob es sich um einen Kuechenbeleg oder Kundenbeleg handelt. Layout
     *                       des ausgedruckten Belegs wird dementsprechend geaendert.
     */
    private void printOrder(Order order, boolean kitchenReceipt) {
        PrinterService printerService = new PrinterService();
        printerService.printOrder(order,  this.getAllItems(), this.getAllTables(), kitchenReceipt);
    }

    /**
     * Ermittelt die aktuelle Verfuegbarkeit eines Items anhand der Wareneingaenge und Warenausgaenge.
     * @param itemID des items, dessen Haeufigkeit ermittelt werden soll.
     * @return aktuelle Verfuegbarkeit des items.
     */
    private int getItemQuantity(int itemID) {
        //Ermitteln der Wareneingaenge
        int itemdeliveries = 0;
        for(Itemdelivery i: this.itemdeliveries) {
            if(i.getItemID() == itemID)
                itemdeliveries += i.getQuantity();
        }
        //Ermitteln der Warenausgaenge
        int itemorders = 0;
        for(Order o: this.orders) {
            for(Item i: o.getItems(this.itemsWithoutQuantity)) {
                if(i.getItemID() == itemID)
                    itemorders++;
            }
        }
        return itemdeliveries - itemorders;
    }

    /**
     * @return eine Liste von allen Items der Datenbank ohne die Anzahl zu bestimmen
     */
    private ArrayList<Item> getAllItemsEmpty()  {
        ArrayList<Item> emptyItems = new ArrayList<>();
        try {
            String query = "SELECT itemID, name, retailprice, available " +
                    "FROM " + dbp.getDatabase() + ".items";
            PreparedStatement pst = connection.prepareStatement(query);
            ResultSet rs = pst.executeQuery();

            while(rs.next()) {
                //get each Item from DB without quantity
                int itemID = rs.getInt("itemID");
                String name = rs.getString("name");
                double retailprice = rs.getFloat("retailprice");
                retailprice = round(retailprice);
                boolean available = rs.getBoolean("available");
                emptyItems.add(new Item(itemID, name, retailprice, available));
            }
            return emptyItems;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Ermittelt die hinzugefuegten Items von einer neuen Order im Vergleich zu einer alten.
     * @param oldOrder die alte Order.
     * @param newOrder die neue Order, die die alte ersetzt.
     * @return Order, die diejenigen Items beinhaltet, die in der neuen Order hinzugefuegt wurden.
     */
    private static Order getDiffOrder(Order oldOrder, Order newOrder) {
        int orderID = oldOrder.getOrderID();
        int tableID = oldOrder.getTable();
        double price = newOrder.getPrice();
        DateTime date = newOrder.getDate();
        boolean paid = newOrder.isPaid();

        String itemIDs = "";
        ArrayList<Integer> oldItemIDs = Order.splitItemIDString(oldOrder.getItems());
        ArrayList<Integer> newItemIDs = Order.splitItemIDString(newOrder.getItems());
        ArrayList<Integer> addedItemIDs = new ArrayList<>();
        ArrayList<Integer> removedItemIDs = new ArrayList<>();

        ArrayList<Integer> allItemIDs = new ArrayList<>();
        allItemIDs.addAll(oldItemIDs);
        allItemIDs.addAll(newItemIDs);

        ArrayList<Integer> alreadyCheckedItemIDs = new ArrayList<>();
        for(Integer id: allItemIDs) {
            if(!alreadyCheckedItemIDs.contains(id)) {
                // Wenn die aktuelle itemID noch nicht ueberprueft wurde,
                // diese den bereits ueberprueften hinzufuegen
                alreadyCheckedItemIDs.add(id);
                // Ermitteln, wie oft die gerade untersuchte id in den alten ids vorkommt
                int oldQuantity = getItemQuantity(id, oldItemIDs);
                int newQuantity = getItemQuantity(id, newItemIDs);
                int diffQuantity = newQuantity - oldQuantity;
                if (diffQuantity > 0) {
                    // Es wurden von der aktuell untersuchten id welche hinzugefuegt
                    for(int i = 0; i < diffQuantity; i++) {
                        addedItemIDs.add(id);
                    }
                } else if(diffQuantity < 0) {
                    // Es wurden von der aktuell untersuchten id welche erntfernt
                    for(int i = 0; i < -diffQuantity; i++) {
                        removedItemIDs.add(id);
                    }
                }
            }
        }
        itemIDs = Order.joinIntIDsIntoString(addedItemIDs);

        return new Order(orderID, itemIDs, tableID, price, date, paid);
    }

    /**
     * Ermittelt die Haeufigkeit einer ID in einer Liste von IDs.
     * @param id ID desjenigen Items, dessen Haeufigkeit in den itemIDs ermittelt werden soll.
     * @param itemIDs ArrayList von Integers mit itemIDs.
     * @return wie oft die id in den itemIDs vorkommt.
     */
    private static int getItemQuantity(int id, ArrayList<Integer> itemIDs) {
        if(!itemIDs.contains(id))
            return 0;
        int quantity = 0;
        for(Integer i: itemIDs) {
            if(i.intValue() == id)
                quantity++;
        }
        return quantity;
    }

    private double round(double number) {
        return (double) Math.round(number * 100d) / 100d;
    }

    //Konverter

    /**
     * Konvertiert ein joda.time.DateTime in einen Timestamp, der in der SQL-Datenbank gespeicher weren kann
     * @param dateTime zu konvertierendes joda.time.DateTime
     * @return Timestamp
     */
    private Object convertJodaDateTimeToSqlTimestamp(DateTime dateTime) {
        // Convert sql-Timestamp to joda.DateTime
        return new Timestamp(dateTime.getMillis());
    }

    /**
     * Konvertiert einen Timestamp aus der SQL-Datenbank in eine joda.time.DateTime
     * @param sqlTimestamp zu konvertierender Timestamp
     * @return jode.time.DateTime
     */
    private DateTime convertSqlTimestampToJodaDateTime(Timestamp sqlTimestamp) {
        //Convert joda.DateTime to sql-Timestamp
        return new DateTime(sqlTimestamp);
    }

    private void logErr(String errorMessage) {
        log(errorMessage, "ERROR");
    }

    private void logInf(String message) {
        log(message, "INFO");
    }

    private void log(String message, String status) {
        String messageStatus = "";
        switch(status) {
            case "INFO": messageStatus = "MYSQL-Info";
            break;
            case "ERROR": messageStatus = "MYSQL-ERROR";
            break;
        }
        String logString = DateTime.now().toString("yyyy-MM-dd kk:mm:ss.SSS") + "  " + messageStatus + " " + message;
        System.out.println(logString);
    }
}