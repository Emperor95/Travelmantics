package com.emperor.travel;

public class TravelDeal {
    private String id;
    private String title;
    private String price;
    private String description;
    private String image_url;

    public TravelDeal() {
    }

//    public TravelDeal(String title, String price, String description, String image_url) {
//        this.title = title;
//        this.price = price;
//        this.description = description;
//        this.image_url = image_url;
//    }

    public TravelDeal(String title, String price, String description) {
        this.title = title;
        this.price = price;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }
}
