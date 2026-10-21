package br.edu.atitus.authapi.entities;

// Persistido pelo ordinal: Admin = 0, Common = 1 (o mesmo valor vai no claim "type" do JWT)
public enum UserType {
    Admin,
    Common
}
