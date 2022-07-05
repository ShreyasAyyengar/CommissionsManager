package dev.shreyasayyengar.bot.database;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class PreparedStatementBuilder {

    private final PreparedStatement theStatement;
    private final AtomicInteger increment = new AtomicInteger(1);

    public PreparedStatementBuilder(Connection databaseConnection, String theQuery) throws SQLException {
        theStatement = databaseConnection.prepareStatement(theQuery);
    }

    public ResultSet executeQuery() throws SQLException {

        ResultSet resultSet = this.theStatement.executeQuery();
        return resultSet;
    }

    public int executeUpdate() throws SQLException {

        int update = this.theStatement.executeUpdate();
        close();

        return update;
    }

    public long executeLargeUpdate() throws SQLException {
        long update = this.theStatement.executeLargeUpdate();

        close();
        return update;
    }

    public void close() throws SQLException {
        this.theStatement.close();
    }

    public PreparedStatement build() {
        return this.theStatement;
    }

    /* SETTERS -------------------------- */

    public PreparedStatementBuilder setNull(int parameterIndex, int sqlType) throws SQLException {

        this.theStatement.setNull(parameterIndex, sqlType);

        return this;
    }

    public PreparedStatementBuilder setBoolean(int parameterIndex, boolean x) throws SQLException {

        this.theStatement.setBoolean(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setByte(int parameterIndex, byte x) throws SQLException {

        this.theStatement.setByte(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setShort(int parameterIndex, short x) throws SQLException {

        this.theStatement.setShort(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setInt(int parameterIndex, int x) throws SQLException {

        this.theStatement.setInt(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setLong(int parameterIndex, long x) throws SQLException {

        this.theStatement.setLong(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setFloat(int parameterIndex, float x) throws SQLException {

        this.theStatement.setFloat(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setDouble(int parameterIndex, double x) throws SQLException {

        this.theStatement.setDouble(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {

        this.theStatement.setBigDecimal(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setString(int parameterIndex, String x) throws SQLException {

        this.theStatement.setString(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setBytes(int parameterIndex, byte[] x) throws SQLException {

        this.theStatement.setBytes(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setDate(int parameterIndex, Date x) throws SQLException {

        this.theStatement.setDate(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setTime(int parameterIndex, Time x) throws SQLException {

        this.theStatement.setTime(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setTimestamp(int parameterIndex, Timestamp x) throws SQLException {

        this.theStatement.setTimestamp(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {

        this.theStatement.setAsciiStream(parameterIndex, x, length);

        return this;
    }

    public PreparedStatementBuilder setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {

        this.theStatement.setBinaryStream(parameterIndex, x, length);

        return this;
    }

    public PreparedStatementBuilder setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {

        this.theStatement.setObject(parameterIndex, x, targetSqlType);

        return this;
    }

    public PreparedStatementBuilder setObject(int parameterIndex, Object x) throws SQLException {

        this.theStatement.setObject(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {

        this.theStatement.setCharacterStream(parameterIndex, reader, length);

        return this;
    }

    public PreparedStatementBuilder setRef(int parameterIndex, Ref x) throws SQLException {

        this.theStatement.setRef(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setBlob(int parameterIndex, Blob x) throws SQLException {

        this.theStatement.setBlob(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setClob(int parameterIndex, Clob x) throws SQLException {

        this.theStatement.setClob(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setArray(int parameterIndex, Array x) throws SQLException {

        this.theStatement.setArray(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {

        this.theStatement.setDate(parameterIndex, x, cal);

        return this;
    }

    public PreparedStatementBuilder setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {

        this.theStatement.setTime(parameterIndex, x, cal);

        return this;
    }

    public PreparedStatementBuilder setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {

        this.theStatement.setTimestamp(parameterIndex, x, cal);

        return this;
    }

    public PreparedStatementBuilder setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {

        this.theStatement.setNull(parameterIndex, sqlType, typeName);

        return this;
    }

    public PreparedStatementBuilder setURL(int parameterIndex, URL x) throws SQLException {

        this.theStatement.setURL(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setRowId(int parameterIndex, RowId x) throws SQLException {

        this.theStatement.setRowId(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setNString(int parameterIndex, String value) throws SQLException {

        this.theStatement.setNString(parameterIndex, value);

        return this;
    }

    public PreparedStatementBuilder setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {

        this.theStatement.setNCharacterStream(parameterIndex, value, length);

        return this;
    }

    public PreparedStatementBuilder setNClob(int parameterIndex, NClob value) throws SQLException {

        this.theStatement.setNClob(parameterIndex, value);

        return this;
    }

    public PreparedStatementBuilder setClob(int parameterIndex, Reader reader, long length) throws SQLException {

        this.theStatement.setClob(parameterIndex, reader, length);

        return this;
    }

    public PreparedStatementBuilder setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {

        this.theStatement.setBlob(parameterIndex, inputStream, length);

        return this;
    }

    public PreparedStatementBuilder setNClob(int parameterIndex, Reader reader, long length) throws SQLException {

        this.theStatement.setNClob(parameterIndex, reader, length);

        return this;
    }

    public PreparedStatementBuilder setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {

        this.theStatement.setSQLXML(parameterIndex, xmlObject);

        return this;
    }

    public PreparedStatementBuilder setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {

        this.theStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);

        return this;
    }

    public PreparedStatementBuilder setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {

        this.theStatement.setAsciiStream(parameterIndex, x, length);

        return this;
    }

    public PreparedStatementBuilder setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {

        this.theStatement.setBinaryStream(parameterIndex, x, length);

        return this;
    }

    public PreparedStatementBuilder setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {

        this.theStatement.setCharacterStream(parameterIndex, reader, length);

        return this;
    }

    public PreparedStatementBuilder setAsciiStream(int parameterIndex, InputStream x) throws SQLException {

        this.theStatement.setAsciiStream(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setBinaryStream(int parameterIndex, InputStream x) throws SQLException {

        this.theStatement.setBinaryStream(parameterIndex, x);

        return this;
    }

    public PreparedStatementBuilder setCharacterStream(int parameterIndex, Reader reader) throws SQLException {

        this.theStatement.setCharacterStream(parameterIndex, reader);

        return this;
    }

    public PreparedStatementBuilder setNCharacterStream(int parameterIndex, Reader value) throws SQLException {

        this.theStatement.setNCharacterStream(parameterIndex, value);

        return this;
    }

    public PreparedStatementBuilder setClob(int parameterIndex, Reader reader) throws SQLException {

        this.theStatement.setClob(parameterIndex, reader);

        return this;
    }

    public PreparedStatementBuilder setBlob(int parameterIndex, InputStream inputStream) throws SQLException {

        this.theStatement.setBlob(parameterIndex, inputStream);

        return this;
    }

    public PreparedStatementBuilder setNClob(int parameterIndex, Reader reader) throws SQLException {

        this.theStatement.setNClob(parameterIndex, reader);

        return this;
    }

    /* SETTERS WITH ATOMIC INTEGER INTEGRATION */

    public PreparedStatementBuilder setNull(int sqlType) throws SQLException {

        this.theStatement.setNull(this.increment.getAndIncrement(), sqlType);

        return this;
    }

    public PreparedStatementBuilder setBoolean(boolean x) throws SQLException {

        this.theStatement.setBoolean(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setByte(byte x) throws SQLException {

        this.theStatement.setByte(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setShort(short x) throws SQLException {

        this.theStatement.setShort(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setInt(int x) throws SQLException {

        this.theStatement.setInt(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setLong(long x) throws SQLException {

        this.theStatement.setLong(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setFloat(float x) throws SQLException {

        this.theStatement.setFloat(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setDouble(double x) throws SQLException {

        this.theStatement.setDouble(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setBigDecimal(BigDecimal x) throws SQLException {

        this.theStatement.setBigDecimal(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setString(String x) throws SQLException {

        this.theStatement.setString(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setBytes(byte[] x) throws SQLException {

        this.theStatement.setBytes(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setDate(Date x) throws SQLException {

        this.theStatement.setDate(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setTime(Time x) throws SQLException {

        this.theStatement.setTime(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setTimestamp(Timestamp x) throws SQLException {

        this.theStatement.setTimestamp(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setAsciiStream(InputStream x, int length) throws SQLException {

        this.theStatement.setAsciiStream(this.increment.getAndIncrement(), x, length);

        return this;
    }

    public PreparedStatementBuilder setBinaryStream(InputStream x, int length) throws SQLException {

        this.theStatement.setBinaryStream(this.increment.getAndIncrement(), x, length);

        return this;
    }

    public PreparedStatementBuilder setObject(Object x, int targetSqlType) throws SQLException {

        this.theStatement.setObject(this.increment.getAndIncrement(), x, targetSqlType);

        return this;
    }

    public PreparedStatementBuilder setObject(Object x) throws SQLException {

        this.theStatement.setObject(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setCharacterStream(Reader reader, int length) throws SQLException {

        this.theStatement.setCharacterStream(this.increment.getAndIncrement(), reader, length);

        return this;
    }

    public PreparedStatementBuilder setRef(Ref x) throws SQLException {

        this.theStatement.setRef(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setBlob(Blob x) throws SQLException {

        this.theStatement.setBlob(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setClob(Clob x) throws SQLException {

        this.theStatement.setClob(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setArray(Array x) throws SQLException {

        this.theStatement.setArray(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setDate(Date x, Calendar cal) throws SQLException {

        this.theStatement.setDate(this.increment.getAndIncrement(), x, cal);

        return this;
    }

    public PreparedStatementBuilder setTime(Time x, Calendar cal) throws SQLException {

        this.theStatement.setTime(this.increment.getAndIncrement(), x, cal);

        return this;
    }

    public PreparedStatementBuilder setTimestamp(Timestamp x, Calendar cal) throws SQLException {

        this.theStatement.setTimestamp(this.increment.getAndIncrement(), x, cal);

        return this;
    }

    public PreparedStatementBuilder setNull(int sqlType, String typeName) throws SQLException {

        this.theStatement.setNull(this.increment.getAndIncrement(), sqlType, typeName);

        return this;
    }

    public PreparedStatementBuilder setURL(URL x) throws SQLException {

        this.theStatement.setURL(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setRowId(RowId x) throws SQLException {

        this.theStatement.setRowId(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setNString(String value) throws SQLException {

        this.theStatement.setNString(this.increment.getAndIncrement(), value);

        return this;
    }

    public PreparedStatementBuilder setNCharacterStream(Reader value, long length) throws SQLException {

        this.theStatement.setNCharacterStream(this.increment.getAndIncrement(), value, length);

        return this;
    }

    public PreparedStatementBuilder setNClob(NClob value) throws SQLException {

        this.theStatement.setNClob(this.increment.getAndIncrement(), value);

        return this;
    }

    public PreparedStatementBuilder setClob(Reader reader, long length) throws SQLException {

        this.theStatement.setClob(this.increment.getAndIncrement(), reader, length);

        return this;
    }

    public PreparedStatementBuilder setBlob(InputStream inputStream, long length) throws SQLException {

        this.theStatement.setBlob(this.increment.getAndIncrement(), inputStream, length);

        return this;
    }

    public PreparedStatementBuilder setNClob(Reader reader, long length) throws SQLException {

        this.theStatement.setNClob(this.increment.getAndIncrement(), reader, length);

        return this;
    }

    public PreparedStatementBuilder setSQLXML(SQLXML xmlObject) throws SQLException {

        this.theStatement.setSQLXML(this.increment.getAndIncrement(), xmlObject);

        return this;
    }

    public PreparedStatementBuilder setObject(Object x, int targetSqlType, int scaleOrLength) throws SQLException {

        this.theStatement.setObject(this.increment.getAndIncrement(), x, targetSqlType, scaleOrLength);

        return this;
    }

    public PreparedStatementBuilder setAsciiStream(InputStream x, long length) throws SQLException {

        this.theStatement.setAsciiStream(this.increment.getAndIncrement(), x, length);

        return this;
    }

    public PreparedStatementBuilder setBinaryStream(InputStream x, long length) throws SQLException {

        this.theStatement.setBinaryStream(this.increment.getAndIncrement(), x, length);

        return this;
    }

    public PreparedStatementBuilder setCharacterStream(Reader reader, long length) throws SQLException {

        this.theStatement.setCharacterStream(this.increment.getAndIncrement(), reader, length);

        return this;
    }

    public PreparedStatementBuilder setAsciiStream(InputStream x) throws SQLException {

        this.theStatement.setAsciiStream(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setBinaryStream(InputStream x) throws SQLException {

        this.theStatement.setBinaryStream(this.increment.getAndIncrement(), x);

        return this;
    }

    public PreparedStatementBuilder setCharacterStream(Reader reader) throws SQLException {

        this.theStatement.setCharacterStream(this.increment.getAndIncrement(), reader);

        return this;
    }

    public PreparedStatementBuilder setNCharacterStream(Reader value) throws SQLException {

        this.theStatement.setNCharacterStream(this.increment.getAndIncrement(), value);

        return this;
    }

    public PreparedStatementBuilder setClob(Reader reader) throws SQLException {

        this.theStatement.setClob(this.increment.getAndIncrement(), reader);

        return this;
    }

    public PreparedStatementBuilder setBlob(InputStream inputStream) throws SQLException {

        this.theStatement.setBlob(this.increment.getAndIncrement(), inputStream);

        return this;
    }

    public PreparedStatementBuilder setNClob(Reader reader) throws SQLException {

        this.theStatement.setNClob(this.increment.getAndIncrement(), reader);

        return this;
    }
}