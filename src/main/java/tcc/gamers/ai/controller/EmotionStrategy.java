package tcc.gamers.ai.controller;

public enum EmotionStrategy{
        DEFENSIVE_REPOSITION, // low focus - high worry - low aggressiveness (move to the closest ally)
        AGGRESSIVE_REPOSITION, // low focus - low worry - high aggressiveness (move to the closes ally with the highest target density)
        SUPPORT_REPOSITION, // high focus - low worry - low aggressiveness (move to the closest ally with the highest target density and the most worry) not the same as aggressive
        PUSH_REPOSITION, // high focus - low worry - high aggressiveness (move to the closest ally with the highest target density and lowest worry)
        COVER_REPOSITION, // whatever focus - high worry - low aggressiveness (move to the closest ally with the most worry and lowest goblin density)
        IDLE, // let the mob do whatever
    }