package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * t_ed_course_task 对象 t_ed_course_task
 *
 * @author fuce
 * @date 2026-09-14
 */
@TableName("t_ed_course_task")
@ApiModel(value = "TEdCourseTask", description = "t_ed_course_task")
public class TEdCourseTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 选课催办事项号 */
    @TableField("item_no")
    @ApiModelProperty(value = "选课催办事项号")
    private String itemNo;

    /** 选课截止那一天最后一刻（定后不许改） */
    @TableField("due_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "选课截止那一天最后一刻（定后不许改）")
    private Date dueAt;

    /** 单条提前催办的自然日数（唯一可调的口径） */
    @TableField("amount")
    @ApiModelProperty(value = "单条提前催办的自然日数（唯一可调的口径）")
    private BigDecimal amount;

    /** 催办事由与受派辅导员记要 */
    @TableField("content")
    @ApiModelProperty(value = "催办事由与受派辅导员记要")
    private String content;

    /** 条目情形 0待派 1已派 2已撤单 3已挂起 */
    @TableField("status")
    @ApiModelProperty(value = "条目情形 0待派 1已派 2已撤单 3已挂起")
    private Integer status;

    /** 关联学生底档 t_ed_student.id（浮出前查在籍） */
    @TableField("student_id")
    @ApiModelProperty(value = "关联学生底档 t_ed_student.id（浮出前查在籍）")
    private Long studentId;

    /** 受派辅导员工号 t_ed_counselor.counselor_no */
    @TableField("assignee")
    @ApiModelProperty(value = "受派辅导员工号 t_ed_counselor.counselor_no")
    private String assignee;

    /** 真派出的时刻（写一次，不许覆盖） */
    @TableField("dispatched_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "真派出的时刻（写一次，不许覆盖）")
    private Date dispatchedAt;

    /** 派出所在档 DAY白天档 PEAK高峰档 */
    @TableField("window_code")
    @ApiModelProperty(value = "派出所在档 DAY白天档 PEAK高峰档")
    private String windowCode;

    /** 派出那一轮的轮次号（档位-时刻） */
    @TableField("round_id")
    @ApiModelProperty(value = "派出那一轮的轮次号（档位-时刻）")
    private String roundId;

    /** 挂起或撤单的原由（留痕） */
    @TableField("stop_reason")
    @ApiModelProperty(value = "挂起或撤单的原由（留痕）")
    private String stopReason;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public Date getDueAt() {
        return dueAt;
    }

    public void setDueAt(Date dueAt) {
        this.dueAt = dueAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Date getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Date dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public String getWindowCode() {
        return windowCode;
    }

    public void setWindowCode(String windowCode) {
        this.windowCode = windowCode;
    }

    public String getRoundId() {
        return roundId;
    }

    public void setRoundId(String roundId) {
        this.roundId = roundId;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
