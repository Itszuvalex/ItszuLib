package com.itszuvalex.itszulib.initialization;

public enum InitializationStage {
    Pre,
    Main,
    Post,
    Complete;

    public InitializationStage Next() {
        switch (this) {
            case Pre:
                return Main;
            case Main:
                return Post;
            case Post:
                return Complete;
        }
        return Complete;
    }
}
