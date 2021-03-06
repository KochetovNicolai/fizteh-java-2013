package ru.fizteh.fivt.students.piakovenko.filemap;

import ru.fizteh.fivt.students.piakovenko.shell.Commands;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Pavel
 * Date: 27.10.13
 * Time: 10:26
 * To change this template use File | Settings | File Templates.
 */
public class Rollback implements Commands {
    private final String name = "rollback";
    private GlobalFileMapState db;

    public Rollback(GlobalFileMapState dataBase) {
        db = dataBase;
    }

    public String getName() {
        return name;
    }

    public void perform(String[] args) throws IOException {
        if (!db.isValidTable()) {
            System.out.println("no table");
            return;
        }
        if (args.length != 1) {
            throw new IOException("Wrong number of arguments! Usage: rollback");
        }
        db.rollback();
    }
}
