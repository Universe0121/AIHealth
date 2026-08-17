package com.aihealth.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aihealth.data.entity.Drug;
import com.aihealth.data.entity.SportRecord;
import com.aihealth.data.model.DrugStatusCount;
import com.aihealth.data.model.DrugTimeInfo;

import java.util.List;

@Dao
public interface AppDao {
    // ========== Drug相关 ==========
    @Insert
    long insertDrug(Drug drug);

    // 【仅优化】保留原有查询，补充排序（与列表展示逻辑一致）
    @Query("SELECT * FROM drug ORDER BY createTime DESC")
    List<Drug> getAllDrugs();

    // 新增方法：获取药品总数
    @Query("SELECT COUNT(*) FROM drug")
    int getDrugCount();

    // 新增方法：根据takeStatus获取已服用药品数量
    // 注意：这里假设"已服用"表示药品是活跃的
    @Query("SELECT COUNT(*) FROM drug WHERE takeStatus = '已服用'")
    int getActiveDrugCount();

    // 新增方法：获取不同状态的药品数量
    @Query("SELECT takeStatus, COUNT(*) as count FROM drug GROUP BY takeStatus")
    List<DrugStatusCount> getDrugCountByStatus();

    // ===================== 新增：适配OCR+诊断单联动的方法 =====================

    // 1. 更新药品（用于修改OCR字段、用量、提醒设置等）
    @Update
    void updateDrug(Drug drug);

    // 2. 根据诊断单ID查询关联药品
    @Query("SELECT * FROM drug WHERE diagnosisId = :diagnosisId")
    List<Drug> getDrugsByDiagnosisId(int diagnosisId);

    // 3. 根据ID精准查询药品（OCR识别后修改字段用）
    @Query("SELECT * FROM drug WHERE id = :drugId LIMIT 1")
    Drug getDrugById(int drugId);

    // 4. 根据药品名称查询（OCR识别后去重用）
    @Query("SELECT * FROM drug WHERE drugName = :drugName LIMIT 1")
    Drug getDrugByName(String drugName);

    // 5. 根据ID删除药品（可选）
    @Query("DELETE FROM drug WHERE id = :drugId")
    void deleteDrugById(int drugId);

    // ========== SportRecord相关 ==========
    @Query("SELECT * FROM sport_record ORDER BY sportTime DESC LIMIT 10")
    List<SportRecord> getRecentSportRecords();

    @Insert
    void insertSportRecord(SportRecord record);

    @Query("SELECT * FROM sport_record ORDER BY sportTime DESC")
    List<SportRecord> getAllSportRecords();

    // 新增：统计数据方法
    @Query("SELECT COUNT(*) FROM sport_record")
    int getSportRecordCount();

    // ================ 补充：适配多时间字段的辅助方法（可选） ================
    // 如需单独查询多时间字段，可新增此方法（非必需，已有getAllDrugs覆盖）
    @Query("SELECT id, drugName, takeTimeList FROM drug WHERE id = :drugId LIMIT 1")
    DrugTimeInfo getDrugTimeListById(int drugId);
}