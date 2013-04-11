/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jamde.estimator;

import jamde.distribution.Distribution;
import java.util.ArrayList;
import jamde.OtherUtils;
import java.util.Arrays;

/**
 * Generalized Cramer. Instead of (F-Fn)^2 it is (F-Fn)^(p/q)
 * 
 * @author honza
 */
public class CramerRationalPowerEstimator extends Estimator{

    public CramerRationalPowerEstimator(int p, int q) {
        this.par = new ArrayList<>();
        this.par.add((double) p); 
        this.par.add((double) q);
    }
    
    @Override
    public double countDistance(Distribution distr, double[] data) {
        int p = (int) Math.round(this.par.get(0));
        int q = (int) Math.round(this.par.get(1));
        double a,b,y, dist = 0;

        Arrays.sort(data);

        for (int i = 0; i < data.length; i++) {
            y = distr.getFunctionValue(data[i]);

            if (y - ((i) * 1.0) / data.length < 0) {
                a = -1.0;
            } else {
                a = 1.0;
            }
            if (y - ((i + 1) * 1.0) / data.length < 0) {
                b = -1.0;
            } else {
                b = 1.0;
            }
            dist = dist + a * Math.pow(Math.abs(y - ((double) i) / data.length), ((double) p) / q + 1.0) - b * Math.pow(Math.abs(y - (i + 1.0) / data.length), ((double) p) / q + 1.0);
        }
        dist =  dist / (((double) p) / q + 1.0);

        return dist;
    }

    @Override
    public String getClassicTableName() {
        return("$ \\mathrm{KC}^\\frac{p}{q}, p="+OtherUtils.num2str(getPar(0)) + ", \\quad q="+OtherUtils.num2str(getPar(1)) + "$");
    }
    
}
