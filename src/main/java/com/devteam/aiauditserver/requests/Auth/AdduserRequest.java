package com.devteam.aiauditserver.requests.Auth;


import com.devteam.aiauditserver.enums.User.Gender;

public class AdduserRequest {

    private String email;
    private String password;
    private String name;
    private String lastname;
    private String firstname;
    private Gender gender;
    private String phonenumber;
    private String landline;
    private String fax;
    private String addresstype;
    private String address;
    private String country;
    private String area;
    private String street;
    private String building;
    private String floor;
    private String unitnumberaddress;
    private String nickname;

    private String city;
    private Boolean active;

    private Boolean status;
    private String company_name;
    private String position;
    private String website;

    private String promocode;

    public AdduserRequest() {
    }

    public AdduserRequest(String email, String password, String name, String lastname, String firstname, Gender gender, String phonenumber, String landline, String fax, String addresstype, String address, String country, String area, String street, String building, String floor, String unitnumberaddress, String nickname, String city, Boolean active, Boolean status, String company_name, String position, String website)
    {
        this.email = email;
        this.password = password;
        this.name = name;
        this.lastname = lastname;
        this.firstname = firstname;
        this.gender = gender;
        this.phonenumber = phonenumber;
        this.landline = landline;
        this.fax = fax;
        this.addresstype = addresstype;
        this.address = address;
        this.country = country;
        this.area = area;
        this.street = street;
        this.building = building;
        this.floor = floor;
        this.unitnumberaddress = unitnumberaddress;
        this.nickname = nickname;
        this.city = city;
        this.active = active;
        this.status = status;
        this.company_name = company_name;
        this.position = position;
        this.website = website;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getPromocode() {
        return promocode;
    }

    public void setPromocode(String promocode) {
        this.promocode = promocode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getLandline() {
        return landline;
    }

    public void setLandline(String landline) {
        this.landline = landline;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getAddresstype() {
        return addresstype;
    }

    public void setAddresstype(String addresstype) {
        this.addresstype = addresstype;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getUnitnumberaddress() {
        return unitnumberaddress;
    }

    public void setUnitnumberaddress(String unitnumberaddress) {
        this.unitnumberaddress = unitnumberaddress;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getCompany_name() {
        return company_name;
    }

    public void setCompany_name(String company_name) {
        this.company_name = company_name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
