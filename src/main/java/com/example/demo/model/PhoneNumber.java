package com.example.demo.model;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumber  {
    @SerializedName("ID")
    @Expose
    private String id;
    @SerializedName("Phone")
    @Expose
    private String phone;
    @SerializedName("Name")
    @Expose
    private String name;
    @SerializedName("Imei")
    @Expose
    private String imei;
    @SerializedName("UserCreate")
    @Expose
    private String userCreate;
    @SerializedName("CreateAt")
    @Expose
    private String createAt;
    @SerializedName("UpdateAt")
    @Expose
    private String updateAt;
    @SerializedName("IsDelete")
    @Expose
    private Boolean isDelete;
    @SerializedName("IsUpdate")
    @Expose
    private Boolean isUpdate;
    @SerializedName("IsInsert")
    @Expose
    private Boolean isInsert;

}
