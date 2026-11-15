package support;

import jakarta.servlet.http.*;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

public final class ServletMocks {
    public final Map<String,Object> sessionValues=new HashMap<>(), attributes=new HashMap<>();
    public final Map<String,String> parameters=new HashMap<>();
    public int error;public String redirect;public boolean invalidated;
    public final java.io.StringWriter body=new java.io.StringWriter();
    public final HttpSession session=proxy(HttpSession.class,(p,m,a)->switch(m.getName()){
        case "getAttribute"->sessionValues.get(a[0]);
        case "setAttribute"->{sessionValues.put((String)a[0],a[1]);yield null;}
        case "removeAttribute"->{sessionValues.remove(a[0]);yield null;}
        case "invalidate"->{invalidated=true;sessionValues.clear();yield null;}
        default->null;
    });
    public final HttpServletRequest request=proxy(HttpServletRequest.class,(p,m,a)->switch(m.getName()){
        case "getSession"->session;
        case "getParameter"->parameters.get(a[0]);
        case "getContextPath"->"/cafe";
        case "getRemoteAddr"->"127.0.0.1";
        case "changeSessionId"->"new-session-id";
        case "setAttribute"->{attributes.put((String)a[0],a[1]);yield null;}
        default->null;
    });
    public final HttpServletResponse response=proxy(HttpServletResponse.class,(p,m,a)->{
        if(m.getName().equals("sendError"))error=(Integer)a[0];
        if(m.getName().equals("sendRedirect"))redirect=(String)a[0];
        if(m.getName().equals("getWriter"))return new java.io.PrintWriter(body);
        return null;
    });
    public void csrf(){parameters.put("csrfToken",utils.AccountCsrf.token(session));}
    public static <T>T proxy(Class<T> type,InvocationHandler handler){
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            if(m.getName().equals("hashCode"))return System.identityHashCode(p);
            if(m.getName().equals("equals"))return p==a[0];
            if(m.getName().equals("toString"))return "Mock "+type.getSimpleName();
            Object result=handler.invoke(p,m,a);
            if(result==null && m.getReturnType().isPrimitive()){
                if(m.getReturnType()==boolean.class)return false;
                if(m.getReturnType()==long.class)return 0L;
                if(m.getReturnType()!=void.class)return 0;
            }
            return result;
        }));
    }
}
