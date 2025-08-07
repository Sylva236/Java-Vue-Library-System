import entities.Book;
import entities.Borrow;
import entities.Card;
import org.postgresql.util.PSQLWarning;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try{
            //检查书籍是否已经存在
            String checkSql = "select * from book where "+
                    "category = ? and title = ? and press = ? "+
                    "and publishYear = ? and  author = ?";
          try(PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1,book.getCategory());
            checkStmt.setString(2,book.getTitle());
            checkStmt.setString(3,book.getPress());
            checkStmt.setInt(4,book.getPublishYear());
            checkStmt.setString(5,book.getAuthor());
            ResultSet rs = checkStmt.executeQuery();
            if(rs.next()){
                conn.rollback();
                return new ApiResult(false, "Book already exists");
            }
          }

          //插入新书
          String insertSql = "insert into book (category, title, press, publishYear, author, "+
                  "price, stock) values (?,?,?,?,?,?,?)";
          try(PreparedStatement insertStmt = conn.prepareStatement(insertSql,PreparedStatement.RETURN_GENERATED_KEYS)){
              insertStmt.setString(1,book.getCategory());
              insertStmt.setString(2,book.getTitle());
              insertStmt.setString(3,book.getPress());
              insertStmt.setInt(4,book.getPublishYear());
              insertStmt.setString(5,book.getAuthor());
              insertStmt.setDouble(6,book.getPrice());
              insertStmt.setInt(7,book.getStock());
              insertStmt.executeUpdate();

              //获取书本id
              ResultSet rs = insertStmt.getGeneratedKeys();
              if(rs.next()){
                  book.setBookId(rs.getInt(1));
              }
          }

          //提交事务
          conn.commit();
          return new ApiResult(true, "Book stored successfully");

        }catch (SQLException e){
            try{
                if(conn!=null){
                    conn.rollback();
                }
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            //检查书籍是否存在
            String checkSql = "select stock from book where bookId = ?";
            int currentStock = 0;
            try(PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1,bookId);
                try(ResultSet rs = checkStmt.executeQuery()) {
                    if(!rs.next()){
                        return new ApiResult(false, "Book with ID " + bookId + " do not exists");
                    }
                    currentStock = rs.getInt("stock");
                }
            }
            //检查库存变化后的值是否合法
            if(currentStock + deltaStock<0){
                return new ApiResult(false, "Invalid stock update: cannot be negative");
            }
            //更新库存
            String updateSql = "update book set stock = stock + ? where bookId = ?";
            try(PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1,deltaStock);
                updateStmt.setInt(2,bookId);
                int RowsAffected = updateStmt.executeUpdate();
                if(RowsAffected==0){
                    return new ApiResult(false, "Failed to update the stock");
                }
            }
            conn.commit();
            return new ApiResult(true, "Stock updated successfully");
        }catch (SQLException e){
            try{
                if(conn!=null) conn.rollback();
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);//开启事务，保证操作的原子性
            //检查是否有重复书籍
            String checkSql = "select 1 from book where category = ? and title = ? "+
                    "and press = ? and publishYear = ? and author = ?";//此处使用1是为了使返回时，只需要有一个常数列来证明该书是否存在即可
            //批量插入语句
            String insertSql = "insert into book "+
                    "(category, title, press, publishYear, author, price, stock)"+
                    " values (?,?,?,?,?,?,?)";

            try{
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                PreparedStatement insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
                //检查重复并准备收集可插入的书籍
                List<Book> booksToAdd = new ArrayList<>();
                for (Book book : books) {
                    checkStmt.setString(1,book.getCategory());
                    checkStmt.setString(2,book.getTitle());
                    checkStmt.setString(3,book.getPress());
                    checkStmt.setInt(4,book.getPublishYear());
                    checkStmt.setString(5,book.getAuthor());

                    try(ResultSet rs = checkStmt.executeQuery()){
                        if(!rs.next()){ //如果无重复的话，就进行插入
                            booksToAdd.add(book);
                        }
                    }
                    checkStmt.clearParameters();
                }

                //进行批量插入行为
                for(Book book : booksToAdd){
                    insertStmt.setString(1,book.getCategory());
                    insertStmt.setString(2,book.getTitle());
                    insertStmt.setString(3,book.getPress());
                    insertStmt.setInt(4,book.getPublishYear());
                    insertStmt.setString(5,book.getAuthor());
                    insertStmt.setDouble(6,book.getPrice());
                    insertStmt.setInt(7,book.getStock());
                    insertStmt.addBatch();
                }
                //进行批处理
                int[] batchResult = insertStmt.executeBatch();

                //获取自增ID，并回填到Book对象
                //此处这样写，就是 try-with-resources，就不需手动关闭资源，也不需catch语句了
                try(ResultSet generatedKeys = insertStmt.getGeneratedKeys()){
                    int index = 0;
                    while(generatedKeys.next()){
                        booksToAdd.get(index++).setBookId(generatedKeys.getInt(1));
                    }
                }

                conn.commit();
                return new ApiResult(true, "Book batch stored successfully");
            }catch (SQLException e){
                try{
                    conn.rollback();
                }catch (SQLException e1){
                    e1.printStackTrace();
                }
                return new ApiResult(false, "Batch insert failed: " + e.getMessage());
            }
        }catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            //检查借还情况
            String checkSql1 = "select 1 from borrow where bookId = ? and returnTime = 0";
            try(PreparedStatement checkStmt = conn.prepareStatement(checkSql1)){
                checkStmt.setInt(1,bookId);
                try(ResultSet rs = checkStmt.executeQuery()){
                    if(rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "Remove failed: " + bookId+"hasn't been returned");
                    }
                }
            }
            //检查书是否存在
            String checkSql2 = "select 1 from book where bookId = ?";
            try(PreparedStatement checkStmt = conn.prepareStatement(checkSql2)){
                checkStmt.setInt(1,bookId);
                try(ResultSet rs = checkStmt.executeQuery()){
                    if(!rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "Remove failed: " + bookId+"dose not exist");
                    }
                }
            }
            //进行级联删除(书本和相关借阅记录)
            String removeSql = "delete from borrow where bookId = ?";
            try(PreparedStatement removeStmt = conn.prepareStatement(removeSql)){
                removeStmt.setInt(1,bookId);
                removeStmt.executeUpdate();
            }
            String removeBookSql = "delete from book where bookId = ?";
            try(PreparedStatement removeBookStmt = conn.prepareStatement(removeBookSql)){
                removeBookStmt.setInt(1,bookId);
                int rowsAffected = removeBookStmt.executeUpdate();
                if(rowsAffected==0){
                    conn.rollback();
                    return new ApiResult(false, "Failed to remove the book");
                }
            }
            conn.commit();
            return new ApiResult(true, "Book and its borrow records removed successfully");
        }catch (SQLException e){
            try{
                if(conn!=null) conn.rollback();
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            //检查书本是否存在
            String checkSql1 = "select 1 from book where bookId = ?";
            try(PreparedStatement checkStmt = conn.prepareStatement(checkSql1)){
                checkStmt.setInt(1,book.getBookId());
                try(ResultSet rs = checkStmt.executeQuery()){
                    if(!rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "Modify failed: " + book.getBookId()+"dose not exist");
                    }
                }
            }
            //更新可修改字段（id和stock不能修改
            String updateSql = "update book set "+
            "category = ?,title = ?, press = ?, publishYear = ?, "+
                    "author = ?, price = ? where bookId = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)){
                updateStmt.setString(1,book.getCategory());
                updateStmt.setString(2,book.getTitle());
                updateStmt.setString(3,book.getPress());
                updateStmt.setInt(4,book.getPublishYear());
                updateStmt.setString(5,book.getAuthor());
                updateStmt.setDouble(6,book.getPrice());
                updateStmt.setInt(7,book.getBookId());

                int rowsAffected = updateStmt.executeUpdate();
                if(rowsAffected==0){
                    conn.rollback();
                    return new ApiResult(false, "Failed to update the book");
                }
            }
            conn.commit();
            return new ApiResult(true, "Book modified successfully");
        }catch (SQLException e){
            try{
                if(conn!=null) conn.rollback();
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        try {
            //构建动态SQL
            StringBuilder querySql = new StringBuilder("select * from book where 1=1");
            List<Object> params = new ArrayList<>();
            //动态添加查询条件
            if(conditions.getCategory()!=null){
                querySql.append(" and category = ?");
                params.add(conditions.getCategory());
            }
            //模糊匹配
            if (conditions.getTitle() != null) {
                querySql.append(" and title like ?");
                params.add("%" + conditions.getTitle() + "%");
            }
            if (conditions.getPress() != null) {
                querySql.append(" and press like ?");
                params.add("%" + conditions.getPress() + "%");
            }
            if (conditions.getAuthor() != null) {
                querySql.append(" and author like ?");
                params.add("%" + conditions.getAuthor() + "%");
            }
            if(conditions.getMinPrice()!=null){
                querySql.append(" and price >= ?");
                params.add(conditions.getMinPrice());
            }
            if(conditions.getMaxPrice()!=null){
                querySql.append(" and price <= ?");
                params.add(conditions.getMaxPrice());
            }
            if(conditions.getMinPublishYear()!=null){
                querySql.append(" and publishYear >= ?");
                params.add(conditions.getMinPublishYear());
            }
            if(conditions.getMaxPublishYear()!=null){
                querySql.append(" and publishYear <= ?");
                params.add(conditions.getMaxPublishYear());
            }

            querySql.append(" order by ")
                    .append(conditions.getSortBy().getValue())
                    .append(" ")
                    .append(conditions.getSortOrder().getValue());
            if (conditions.getSortBy() != Book.SortColumn.BOOK_ID) {
                querySql.append(", bookId ASC");
            }

            //执行查询
            try(PreparedStatement stmt = conn.prepareStatement(querySql.toString())){
                //绑定参数
                for(int i=0; i<params.size(); i++){
                    stmt.setObject(i+1,params.get(i));
                }
                //处理结果集
                List<Book> books = new ArrayList<>();
                try(ResultSet rs = stmt.executeQuery()){
                    while(rs.next()){
                        Book book = new Book();
                        book.setBookId(rs.getInt(1));
                        book.setCategory(rs.getString(2));
                        book.setTitle(rs.getString(3));
                        book.setPress(rs.getString(4));
                        book.setPublishYear(rs.getInt(5));
                        book.setAuthor(rs.getString(6));
                        book.setPrice(rs.getDouble(7));
                        book.setStock(rs.getInt(8));
                        books.add(book);
                    }
                }
                //封装结果
                BookQueryResults Results = new BookQueryResults(books);
                Results.setCount(books.size());
                return new ApiResult(true, Results);
            }
        }catch (SQLException e){
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            //检查库存是否充足
            String stockSql = "select stock from book where bookId = ? for update";
            try(PreparedStatement stmt = conn.prepareStatement(stockSql)){
                stmt.setInt(1,borrow.getBookId());
                try(ResultSet rs = stmt.executeQuery()){
                    if(!rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "Book does not exist");
                    }
                    int stock = rs.getInt("stock");
                    if(stock <= 0){
                        conn.rollback();
                        return new ApiResult(false, "Insufficient stock");
                    }
                }
            }
            //检查是否已有未归还记录
            String borrowSql1 = "select 1 from Borrow where bookId = ? and cardId = ? and returnTime = 0";
            try(PreparedStatement stmt = conn.prepareStatement(borrowSql1)){
                stmt.setInt(1,borrow.getBookId());
                stmt.setInt(2,borrow.getCardId());
                try(ResultSet rs = stmt.executeQuery()){
                    if(rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "You have an unreturned record of this book");
                    }
                }
            }
            //减少库存
            String updateSql = "update book set stock = stock - 1 where bookId = ?";
            try(PreparedStatement stmt = conn.prepareStatement(updateSql)){
                stmt.setInt(1,borrow.getBookId());
                int affected = stmt.executeUpdate();
                if(affected == 0){
                    conn.rollback();
                    return new ApiResult(false, "Stock update failed");
                }
            }
            //创造借阅记录
            String borrowSql2 = "insert into Borrow (bookId, cardId, borrowTime, returnTime) values (?, ?, ?, 0)";
            try(PreparedStatement stmt = conn.prepareStatement(borrowSql2)){
                stmt.setInt(1,borrow.getBookId());
                stmt.setInt(2,borrow.getCardId());
                stmt.setLong(3,borrow.getBorrowTime());
                stmt.executeUpdate();
            }
            conn.commit();
            return new ApiResult(true, "Book successfully borrowed");
        }catch (SQLException e){
            try{
                if(conn!=null) conn.rollback();
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            //检查是否存在未归还的借阅记录
            String borrowSql = "select 1 from Borrow where bookId = ? and cardId = ? and returnTime = 0";
            try(PreparedStatement stmt = conn.prepareStatement(borrowSql)){
                stmt.setInt(1,borrow.getBookId());
                stmt.setInt(2,borrow.getCardId());
                try(ResultSet rs = stmt.executeQuery()){
                    if(!rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "No active borrow record found");
                    }
                    // 添加时间验证
                    long borrowTime = borrow.getBorrowTime();
                    long returnTime = borrow.getReturnTime();
                    if(borrowTime == 0 ){
                        conn.rollback();
                        return new ApiResult(false, "Borrow time cannot be 0");
                    }
                    if(returnTime <= borrowTime) {
                        conn.rollback();
                        return new ApiResult(false, "Return time must be after borrow time");
                    }
                }
            }
            //更新归还时间
            String updateSql = "update Borrow set returnTime = ? where bookId = ? and cardId = ? and returnTime = 0";
            try(PreparedStatement stmt = conn.prepareStatement(updateSql)){
                stmt.setLong(1,borrow.getReturnTime());
                stmt.setInt(2,borrow.getBookId());
                stmt.setInt(3,borrow.getCardId());
                int rowsAffected = stmt.executeUpdate();
                if(rowsAffected == 0){
                    conn.rollback();
                    return new ApiResult(false, "Failed to update return time");
                }
            }
            //处理库存
            String stockSql = "update book set stock = stock + 1 where bookId = ?";
            try(PreparedStatement stmt = conn.prepareStatement(stockSql)){
                stmt.setInt(1,borrow.getBookId());
                int affected = stmt.executeUpdate();
                if(affected == 0){
                    conn.rollback();
                    return new ApiResult(false, "Stock update failed");
                }
            }
            conn.commit();
            return new ApiResult(true, "Book successfully returned");
        }catch (SQLException e){
            try{
                if(conn!=null) conn.rollback();
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        try {
            //构建带排序的查询SQL
            String querySql = "select b.*, bk.* "+
                    "from Borrow b "+
                    "join book bk on b.bookId = bk.bookId "+
                    "where b.cardId = ? "+
                    "order by b.borrowTime desc, b.bookId asc";
            //执行查询
            try(PreparedStatement stmt = conn.prepareStatement(querySql)){
                stmt.setInt(1,cardId);
                //处理结果集
                List<BorrowHistories.Item>items = new ArrayList<>();
                try(ResultSet rs = stmt.executeQuery()){
                    while(rs.next()){
                        //构造book对象
                        Book book = new Book();
                        book.setBookId(rs.getInt("bookId"));
                        book.setCategory(rs.getString("category"));
                        book.setTitle(rs.getString("title"));
                        book.setPress(rs.getString("press"));
                        book.setPublishYear(rs.getInt("publishYear"));
                        book.setAuthor(rs.getString("author"));
                        book.setPrice(rs.getDouble("price"));
                        book.setStock(rs.getInt("stock"));
                        //构造borrow对象
                        Borrow borrow = new Borrow();
                        borrow.setBookId(rs.getInt("bookId"));
                        borrow.setCardId(rs.getInt("cardId"));
                        borrow.setReturnTime(rs.getLong("returnTime"));
                        borrow.setBorrowTime(rs.getLong("borrowTime"));
                        //创建BorrowHistories.Item
                        items.add(new BorrowHistories.Item(cardId,book,borrow));
                    }
                }
                //封装最终结果
                return new ApiResult(true, new BorrowHistories(items));
            }
        }catch (SQLException e){
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            //检查卡是否已经存在
            String cardSql = "select cardId from Card where name = ? and department = ? and type = ?";
            try(PreparedStatement stmt = conn.prepareStatement(cardSql)){
                stmt.setString(1,card.getName());
                stmt.setString(2,card.getDepartment());
                stmt.setString(3,card.getType().getStr());
                try(ResultSet rs = stmt.executeQuery()){
                    if(rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "Card already exists");
                    }
                }
            }
            //插入新卡
            String insertSql = "insert into Card (name, department, type) values(?,?,?)";
            try(PreparedStatement stmt = conn.prepareStatement(insertSql,PreparedStatement.RETURN_GENERATED_KEYS)){
                stmt.setString(1,card.getName());
                stmt.setString(2,card.getDepartment());
                stmt.setString(3,card.getType().getStr());

                int RowsAffected = stmt.executeUpdate();
                if(RowsAffected == 0){
                    conn.rollback();
                    return new ApiResult(false, "Failed to insert card");
                }
                //获取生成的cardId，并回填
                try(ResultSet rs = stmt.getGeneratedKeys()){
                    if(rs.next()){
                        card.setCardId(rs.getInt(1));
                    }else{
                        conn.rollback();
                        return new ApiResult(false, "Failed to get card ID");
                    }
                }
            }
            conn.commit();
            return new ApiResult(true, "Card successfully registered");
        }catch (SQLException e){
            try{
                if(conn!=null) conn.rollback();
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            //检查是否存在未归还书籍
            String borrowSql = "select 1 from Borrow where cardId = ? and returnTime = 0";
            try(PreparedStatement stmt = conn.prepareStatement(borrowSql)){
                stmt.setInt(1,cardId);
                try(ResultSet rs = stmt.executeQuery()){
                    if(rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "Cannot remove card: user has unreturned record");
                    }
                }
            }
            //检查卡片是否存在
            String checkSql = "select 1 from Card where cardId = ?";
            try(PreparedStatement stmt = conn.prepareStatement(checkSql)){
                stmt.setInt(1,cardId);
                try(ResultSet rs = stmt.executeQuery()){
                    if(!rs.next()){
                        conn.rollback();
                        return new ApiResult(false, "Card does not exist");
                    }
                }
            }
            //删除卡片（先删除记录，再删除卡片
            String deleteBorrowSql = "delete from Borrow where cardId = ?";
            try(PreparedStatement stmt = conn.prepareStatement(deleteBorrowSql)){
                stmt.setInt(1,cardId);
                stmt.executeUpdate();
            }
            String deleteCardSql = "delete from Card where cardId = ?";
            try(PreparedStatement stmt = conn.prepareStatement(deleteCardSql)){
                stmt.setInt(1,cardId);
                int rowsAffected = stmt.executeUpdate();
                if(rowsAffected == 0){
                    conn.rollback();
                    return new ApiResult(false, "No card was deleted");
                }
            }
            conn.commit();
            return new ApiResult(true, "Card successfully removed");

        }catch (SQLException e){
            try{
                if(conn!=null) conn.rollback();
            }catch(SQLException e1){
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        try {
            //按ID升序构建查询SQL
            String querySql = "select * from Card order by cardId ASC";
            //执行查询
            try(PreparedStatement stmt = conn.prepareStatement(querySql)){
                List<Card> cards = new ArrayList<>();
                //处理结果集
                try(ResultSet rs = stmt.executeQuery()){
                    while(rs.next()){
                        Card card = new Card();
                        card.setCardId(rs.getInt("cardId"));
                        card.setName(rs.getString("name"));
                        card.setDepartment(rs.getString("department"));
                        card.setType(Card.CardType.fromString(rs.getString("type")));
                        cards.add(card);
                    }
                }
                //封装结果
                return new ApiResult(true, new CardList(cards) );
            }
        }catch (SQLException e){
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ApiResult modifyCard(Card card) {
        Connection conn = connector.getConn();
        try {
            conn.setAutoCommit(false);

            // 检查卡片是否存在
            String checkSql = "select 1 from Card where cardId = ?";
            try(PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setInt(1, card.getCardId());
                try(ResultSet rs = stmt.executeQuery()) {
                    if(!rs.next()) {
                        conn.rollback();
                        return new ApiResult(false, "Card does not exist");
                    }
                }
            }

            // 检查是否存在相同信息的其他卡片
            String duplicateSql = "select 1 from Card where name = ? and department = ? and type = ? and cardId != ?";
            try(PreparedStatement stmt = conn.prepareStatement(duplicateSql)) {
                stmt.setString(1, card.getName());
                stmt.setString(2, card.getDepartment());
                stmt.setString(3, card.getType().getStr());
                stmt.setInt(4, card.getCardId());
                try(ResultSet rs = stmt.executeQuery()) {
                    if(rs.next()) {
                        conn.rollback();
                        return new ApiResult(false, "A card with the same information already exists");
                    }
                }
            }

            // 更新卡片信息
            String updateSql = "update Card set name = ?, department = ?, type = ? where cardId = ?";
            try(PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, card.getName());
                stmt.setString(2, card.getDepartment());
                stmt.setString(3, card.getType().getStr());
                stmt.setInt(4, card.getCardId());
                
                int rowsAffected = stmt.executeUpdate();
                if(rowsAffected == 0) {
                    conn.rollback();
                    return new ApiResult(false, "Failed to update card");
                }
            }

            conn.commit();
            return new ApiResult(true, "Card successfully modified");
        } catch (SQLException e) {
            try {
                if(conn != null) conn.rollback();
            } catch(SQLException e1) {
                e1.printStackTrace();
            }
            return new ApiResult(false, "Database error: " + e.getMessage());
        }
    }

}
