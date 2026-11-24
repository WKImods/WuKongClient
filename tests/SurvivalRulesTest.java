import dev.wukong.client.SurvivalRules;
public class SurvivalRulesTest {
    public static void main(String[] args) {
        assert SurvivalRules.remaining(100,95)==5;
        assert SurvivalRules.remaining(100,120)==0;
        assert SurvivalRules.wornOut(100,95,5);
        assert !SurvivalRules.wornOut(100,94,5);
        assert !SurvivalRules.wornOut(0,0,1);
        assert SurvivalRules.shouldEat(14,14);
        assert !SurvivalRules.shouldEat(15,14);
        assert SurvivalRules.miningScore(7,true)>SurvivalRules.miningScore(4,true);
        assert SurvivalRules.miningScore(3,true)>SurvivalRules.miningScore(3,false);
        System.out.println("SurvivalRulesTest: 9 assertions OK");
    }
}
