package com.aihealth.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.aihealth.data.entity.DietRecord;
import com.aihealth.data.entity.NutritionItem;
import java.util.List;

@Dao
public interface DietDao {

    // ========== 饮食记录操作 ==========

    /**
     * 插入饮食记录
     */
    @Insert
    void insertDietRecord(DietRecord record);

    /**
     * 获取所有饮食记录（按时间倒序）
     */
    @Query("SELECT * FROM diet_records ORDER BY timestamp DESC")
    List<DietRecord> getAllDietRecords();

    /**
     * 清空饮食记录表
     */
    @Query("DELETE FROM diet_records")
    void deleteAllDietRecords();

    // ========== 营养数据操作 ==========

    /**
     * 插入营养数据
     */
    @Insert
    void insertNutritionItem(NutritionItem item);

    /**
     * 根据食物名称获取营养信息
     */
    @Query("SELECT * FROM nutrition_data WHERE foodName = :foodName")
    NutritionItem getNutritionInfo(String foodName);

    /**
     * 获取所有营养数据
     */
    @Query("SELECT * FROM nutrition_data ORDER BY foodName")
    List<NutritionItem> getAllNutritionItems();

    /**
     * 清空营养数据表
     */
    @Query("DELETE FROM nutrition_data")
    void deleteAllNutritionItems();

}