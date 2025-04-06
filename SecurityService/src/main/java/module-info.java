module SecurityService {
    requires transitive com.udasecurity.image.service;
    requires transitive com.miglayout.swing;
    requires java.desktop;
    requires java.prefs;
    requires transitive com.google.gson;
    requires transitive dev.mccue.guava.collect;
    requires transitive dev.mccue.guava.reflect;

    opens com.udasecurity.security.data;
}