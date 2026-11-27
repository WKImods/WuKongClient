package dev.wukong.client;

import java.util.function.IntFunction;

/** Pure hotbar selection. The same speed/drop scoring is used by ordinary Auto Tool. */
final class MiningToolChoice {
    record Candidate(float speed,boolean correctDrops,boolean damageable,int remaining,boolean empty) {}
    static int choose(int selected,boolean automatic,boolean requiresCorrect,int reserve,IntFunction<Candidate> at){
        if(selected<0||selected>=9)return -1;
        int best=-1;
        double bestScore=Double.NEGATIVE_INFINITY;
        for(int i=0;i<9;i++){
            if(!automatic&&i!=selected)continue;
            Candidate c=at.apply(i);
            if(c==null||(c.damageable()&&c.remaining()<=Math.max(1,reserve)))continue;
            if(requiresCorrect&&!c.correctDrops())continue;
            // Do not waste an unsuitable damageable tool on hand-mineable blocks.
            if(!c.correctDrops()&&c.speed()<=1.01f&&c.damageable())continue;
            double score=SurvivalRules.miningScore(c.speed(),c.correctDrops());
            // When no useful tool exists, prefer empty hand over a held item.
            if(c.empty())score+=0.01;
            if(score>bestScore+0.001||(Math.abs(score-bestScore)<0.001&&i==selected)){
                best=i;bestScore=score;
            }
        }
        return best;
    }
    private MiningToolChoice(){}
}
