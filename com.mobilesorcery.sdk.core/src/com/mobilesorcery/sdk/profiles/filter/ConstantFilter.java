package com.mobilesorcery.sdk.profiles.filter;

import org.eclipse.ui.IMemento;

import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.profiles.IProfile;
import com.mobilesorcery.sdk.profiles.filter.elementfactories.ConstantFilterFactory;

public class ConstantFilter extends AbstractDeviceFilter {
    
    public interface RelationalOp {
        public boolean evaluate(long lhs, long rhs);
        public String getDescription();
    }
    
    public final static RelationalOp GT = new RelationalOp() {
        public boolean evaluate(long lhs, long rhs) {
            return lhs > rhs;
        }
        
        public String getDescription() {
            return "Greater than (>)";
        }
        
        public String toString() {
            return ">";
        }

    };
    
    public final static RelationalOp LT = new RelationalOp() {
        public boolean evaluate(long lhs, long rhs) {
            return lhs < rhs;
        }        

        public String getDescription() {
            return "Less than (<)";
        }
        
        public String toString() {
            return "<";
        }

    };
    
    public final static RelationalOp EQ = new RelationalOp() {
        public boolean evaluate(long lhs, long rhs) {
            return lhs == rhs;
        }        

        public String getDescription() {
            return "Equals (==)";
        }
        
        public String toString() {
            return "=";
        }
    };
    
    public final static RelationalOp NEQ = new RelationalOp() {
        public boolean evaluate(long lhs, long rhs) {
            return lhs != rhs;
        }        
        
        public String getDescription() {
            return "Not equals (!=)";
        }

        public String toString() {
            return "!=";
        }
    };

    public static final RelationalOp[] ALL_OPS = new RelationalOp[] {
        GT, LT, EQ, NEQ
    };

    
    public final static String DYNAMIC = "DYNAMIC";

    private String constantFeature;
    private long threshold;
    private RelationalOp op;
    
    public ConstantFilter() {
        
    }
    
    public static RelationalOp getOp(String op) {
        for (int i = 0; i < ALL_OPS.length; i++) {
            if (ALL_OPS[i].toString().equals(op)) {
                return ALL_OPS[i];
            }
        }
        
        return null;
    }

    public void setConstantFeature(String constantFeature) {
        this.constantFeature = constantFeature;
    }
    
    public String getConstantFeature() {
        return constantFeature;
    }
    
    public void setThreshold(long threshold){
        this.threshold = threshold;
    }
    
    public long getThreshold() {
        return threshold;
    }

    public void setRelationalOp(RelationalOp op) {
        this.op = op;
    }
    
    public RelationalOp getRelationalOp() {
        return op;
    }

    public boolean acceptProfile(IProfile profile) {
        Object constantFeatureValue = profile.getProperties().get(constantFeature);
        if (DYNAMIC.equals(constantFeatureValue)) {
            return true;
        }
        
        if (constantFeatureValue instanceof Long) {
            long value = ((Long)constantFeatureValue).longValue();
            return op.evaluate(value, threshold);
        }
        
        return true;
    }
    
    public String toString() {
        return MoSyncTool.getDefault().getFeatureDescription(constantFeature) + " " + op.toString() + " " + threshold; 
    }

    public void saveState(IMemento memento) {
        memento.putString("constant-feature", constantFeature);
        memento.putString("op", op.toString());
        memento.putString("threshold", Long.toString(threshold, 10));
    }
    
    public String getFactoryId() {
        return ConstantFilterFactory.ID;
    }

}
