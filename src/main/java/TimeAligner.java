import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class TimeAligner {

	private static boolean _aligned = false;
    private static int _timeDifference = 0;

    public static long GetSteamTime()
    {
        if (!TimeAligner._aligned)
        {
            TimeAligner.AlignTime();
        }
        return Util.GetSystemUnixTime() + _timeDifference;
    }

    public static long GetSteamTimeAsync()
    {
        if (!TimeAligner._aligned)
        {
             TimeAligner.AlignTime();
        }
        return Util.GetSystemUnixTime() + _timeDifference;
    }

    public static void AlignTime()
    {
        long currentTime = Util.GetSystemUnixTime();
       
            try
            {
            	Map<String,String> data = new HashMap<String,String>();
            	data.put("steamid","0");
                String response = SteamWeb.Request(APIEndpoints.TWO_FACTOR_TIME_QUERY,"POST",data,null,null);                               
            	Gson gson = new GsonBuilder().serializeNulls().create();	
            	TimeQuery query = gson.fromJson(response, TimeQuery.class);        
                TimeAligner._timeDifference = (int)(query.Response.ServerTime - currentTime);
                TimeAligner._aligned = true;
            }
            catch (Exception e)
            {
                return;
            }
        
    }


    public class TimeQuery
    {
    	@SerializedName("response")
        public  TimeQueryResponse Response;

        public class TimeQueryResponse
        {
        	@SerializedName("server_time")
            public long ServerTime;
        }
        
    }
}
