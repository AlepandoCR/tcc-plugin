package tcc.gamers.skript;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface OnMutableSave<ObjectType> {

    void onSave(@NotNull ObjectType value);
}
