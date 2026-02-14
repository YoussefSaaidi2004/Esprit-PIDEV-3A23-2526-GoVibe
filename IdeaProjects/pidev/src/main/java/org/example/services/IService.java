package org.example.services;

import java.sql.SQLException;
import java.util.List;

public interface IService<T> {
    public void insert(T t) throws SQLException;
    public void delete(int id) throws SQLException;
    public void update(T t) throws SQLException;
    List<T> show() throws SQLException;



}
