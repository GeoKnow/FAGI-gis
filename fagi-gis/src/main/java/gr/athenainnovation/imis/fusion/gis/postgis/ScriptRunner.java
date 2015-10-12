/*
 * Slightly modified version of the org.ibatis.jdbc.ScriptRunner class from the MyBatis project. Changed RuntimeSqlException to RuntimeException. Added javadoc.
 * by Thomas Maroulis, April 2013
 */

/*
 * Changed logging to use log4j.
 * Thomas Maroulis, August 2013
 */

/*
 *    Copyright 2009-2012 The MyBatis Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gr.athenainnovation.imis.fusion.gis.postgis;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 * Handles parsing of sql scripts from file and executing them on a database
 * 
 */
public class ScriptRunner {

    private static final Logger LOG = Logger.getLogger(ScriptRunner.class);

    private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    private static final String DEFAULT_DELIMITER = ";";

    private Connection connection;

    private boolean stopOnError;
    private boolean autoCommit;
    private boolean sendFullScript;
    private boolean removeCRs;
    private boolean escapeProcessing = true;

    private String delimiter = DEFAULT_DELIMITER;
    private boolean fullLineDelimiter = false;

    /**
     * Initialises new ScriptRunner with given connection
     *
     * @param connection a connection to the database
     */
    public ScriptRunner(Connection connection) {
        this.connection = connection;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    /**
     * Set true to send entire script to database at once
     *
     * @param sendFullScript
     */
    public void setSendFullScript(boolean sendFullScript) {
        this.sendFullScript = sendFullScript;
    }

    public void setRemoveCRs(boolean removeCRs) {
        this.removeCRs = removeCRs;
    }

    /**
     * @since 3.1.1
     */
    public void setEscapeProcessing(boolean escapeProcessing) {
        this.escapeProcessing = escapeProcessing;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setFullLineDelimiter(boolean fullLineDelimiter) {
        this.fullLineDelimiter = fullLineDelimiter;
    }

    /**
     * Parses and executes given sql script on db
     *
     * @param reader reader to sql script
     */
    public void runScript(Reader reader) {
        setAutoCommit();

        try {
            if (sendFullScript) {
                executeFullScript(reader);
            } else {
                executeLineByLine(reader);
            }
        } finally {
            rollbackConnection();
        }
    }

    private void executeFullScript(Reader reader) {
        StringBuilder script = new StringBuilder();
        try {
            BufferedReader lineReader = new BufferedReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                script.append(line);
                script.append(LINE_SEPARATOR);
            }
            executeStatement(script.toString());
            commitConnection();
        } catch (Exception e) {
            String message = "Error executing: " + script + ".  Cause: " + e;
            printlnError(message);
            throw new RuntimeException(message, e);
        }
    }

    private void executeLineByLine(Reader reader) {
        StringBuilder command = new StringBuilder();
        try {
            BufferedReader lineReader = new BufferedReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                command = handleLine(command, line);
            }
            commitConnection();
            checkForMissingLineTerminator(command);
        } catch (Exception e) {
            String message = "Error executing: " + command + ".  Cause: " + e;
            printlnError(message);
            throw new RuntimeException(message, e);
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (Exception e) {
            // ignore
        }
    }

    private void setAutoCommit() {
        try {
            if (autoCommit != connection.getAutoCommit()) {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
        }
    }

    private void commitConnection() {
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (Throwable t) {
            throw new RuntimeException("Could not commit transaction. Cause: " + t, t);
        }
    }

    private void rollbackConnection() {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    private void checkForMissingLineTerminator(StringBuilder command) {
        if (command != null && command.toString().trim().length() > 0) {
            throw new RuntimeException("Line missing end-of-line terminator (" + delimiter + ") => " + command);
        }
    }

    private StringBuilder handleLine(StringBuilder command, String line) {
        String trimmedLine = line.trim();
        if (lineIsComment(trimmedLine)) {
            println(trimmedLine);
        } else if (commandReadyToExecute(trimmedLine)) {
            command.append(line.substring(0, line.lastIndexOf(delimiter)));
            command.append(LINE_SEPARATOR);
            println(command);
            executeStatement(command.toString());
            command.setLength(0);
        } else if (trimmedLine.length() > 0) {
            command.append(line);
            command.append(LINE_SEPARATOR);
        }
        return command;
    }

    private boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--");
    }

    private boolean commandReadyToExecute(String trimmedLine) {
        // issue #561 remove anything after the delimiter
        return !fullLineDelimiter && trimmedLine.contains(delimiter) || fullLineDelimiter && trimmedLine.equals(delimiter);
    }

    private void executeStatement(String command) {
        boolean hasResults = false;
        Statement statement;
        try {
            statement = connection.createStatement();

            statement.setEscapeProcessing(escapeProcessing);
            String sql = command;
            if (removeCRs) {
                sql = sql.replaceAll("\r\n", "\n");
            }
            if (stopOnError) {
                hasResults = statement.execute(sql);
            } else {
                try {
                    hasResults = statement.execute(sql);
                } catch (SQLException e) {
                    String message = "Error executing: " + command + ".  Cause: " + e;
                    printlnError(message);
                    throw new RuntimeException(message, e);
                }
            }

            printResults(statement, hasResults);
            try {
                statement.close();
            } catch (Exception e) {
                // Ignore to workaround a bug in some connection pools
            }
        } catch (SQLException ex) {
             LOG.trace("SQLException in Script Runner");
             LOG.debug("SQLException in Script Runner ; " + ex);
        }
    }

    private void printResults(Statement statement, boolean hasResults) {
        try {
            if (hasResults) {
                ResultSet rs = statement.getResultSet();
                if (rs != null) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    for (int i = 0; i < cols; i++) {
                        String name = md.getColumnLabel(i + 1);
                        print(name + "\t");
                    }
                    println("");
                    while (rs.next()) {
                        for (int i = 0; i < cols; i++) {
                            String value = rs.getString(i + 1);
                            print(value + "\t");
                        }
                        println("");
                    }
                }
            }
        } catch (SQLException e) {
            printlnError("Error printing results: " + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void print(Object o) {
        LOG.info(o.toString());
    }

    private void println(Object o) {
        LOG.info(o.toString());
    }

    private void printlnError(Object o) {
        LOG.error(o.toString());
    }

}
