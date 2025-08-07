package utils;

public class MysqlInitializer implements DBInitializer {

    @Override
    public String sqlDropBook() {
        return "drop table if exists `book`;";
    }

    @Override
    public String sqlDropCard() {
        return "drop table if exists `card`;";
    }

    @Override
    public String sqlDropBorrow() {
        return "drop table if exists `borrow`;";
    }

    @Override
    public String sqlCreateBook() {
        return "create table `book` (\n" +
                "    `bookId` int not null auto_increment,\n" +
                "    `category` varchar(63) not null,\n" +
                "    `title` varchar(63) not null,\n" +
                "    `press` varchar(63) not null,\n" +
                "    `publishYear` int not null,\n" +
                "    `author` varchar(63) not null,\n" +
                "    `price` decimal(7, 2) not null default 0.00,\n" +
                "    `stock` int not null default 0,\n" +
                "    primary key (`bookId`),\n" +
                "    unique (`category`, `press`, `author`, `title`, `publishYear`)\n" +
                ") engine=innodb charset=utf8mb4;";
    }

    @Override
    public String sqlCreateCard() {
        return "create table `card` (\n" +
                "    `cardId` int not null auto_increment,\n" +
                "    `name` varchar(63) not null,\n" +
                "    `department` varchar(63) not null,\n" +
                "    `type` char(1) not null,\n" +
                "    primary key (`cardId`),\n" +
                "    unique (`department`, `type`, `name`),\n" +
                "    check ( `type` in ('T', 'S') )\n" +
                ") engine=innodb charset=utf8mb4;";
    }

    @Override
    public String sqlCreateBorrow() {
        return "create table `borrow` (\n" +
                "  `cardId` int not null,\n" +
                "  `bookId` int not null,\n" +
                "  `borrowTime` bigint not null,\n" +
                "  `returnTime` bigint not null default 0,\n" +
                "  primary key (`cardId`, `bookId`, `borrowTime`),\n" +
                "  foreign key (`cardId`) references `card`(`cardId`) on delete cascade on update cascade,\n" +
                "  foreign key (`bookId`) references `book`(`bookId`) on delete cascade on update cascade\n" +
                ") engine=innodb charset=utf8mb4;";
    }
}
