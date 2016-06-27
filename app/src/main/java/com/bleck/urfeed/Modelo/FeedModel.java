package com.bleck.urfeed.Modelo;

/**
 * Created by Carlos on 26/06/2016.
 */
public class FeedModel {
    private String name;
    private String link;
    private String bckImg;

    public FeedModel(String name, String link, String bckImg) {
        this.setName(name);
        this.setLink(link);
        this.setBckImg(bckImg);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getBckImg() {
        return bckImg;
    }

    public void setBckImg(String bckImg) {
        this.bckImg = bckImg;
    }
}

