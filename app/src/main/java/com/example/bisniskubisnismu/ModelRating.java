package com.example.bisniskubisnismu;

public class ModelRating {
    private String nama;
    private String comment;
    private String foto;

    public ModelRating(String nama, String comment, String foto) {
        this.nama = nama;
        this.comment = comment;
        this.foto = foto;

    }

    public String getNama(){
        return nama;
    }

    public String getComment(){
        return comment;
    }

    public String getFoto(){
        return foto;
    }
}
