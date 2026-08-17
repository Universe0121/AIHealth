package com.aihealth.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aihealth.data.entity.DiagnosisEntity;

import java.util.List;

@Dao
public interface DiagnosisDao {

    @Insert
    long insert(DiagnosisEntity entity);

    @Update
    void update(DiagnosisEntity entity);

    @Query("SELECT * FROM diagnosis ORDER BY timestamp DESC")
    List<DiagnosisEntity> getAll();

    @Query("SELECT * FROM diagnosis WHERE id = :id LIMIT 1")
    DiagnosisEntity getById(int id);

    @Query("DELETE FROM diagnosis WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT COUNT(*) FROM diagnosis")
    int getCount();

    // 按日期范围查询（用于统计或报告）
    @Query("SELECT * FROM diagnosis WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<DiagnosisEntity> getBetweenDates(long start, long end);
}