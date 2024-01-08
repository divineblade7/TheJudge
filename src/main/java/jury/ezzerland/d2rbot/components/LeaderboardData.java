package jury.ezzerland.d2rbot.components;

import jury.ezzerland.d2rbot.Environment;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static jury.ezzerland.d2rbot.TheJudge.BOT;

public class LeaderboardData {
    private int participantsThisMonth = 0, participantsAllTime = 0, hostsThisMonth = 0, hostsAllTime = 0, runsAllTime = 0, runsThisMonth = 0;
    private String topHostAllTime = "",topParticipantAllTime = "",hostWithMostAllTime = "",topHostMonthly = "",topParticipantMonthly = "",hostWithMostMonthly = "";
    private SlashCommandInteractionEvent event;

    public LeaderboardData (SlashCommandInteractionEvent event) {
        this.event = event;
        setParticipationCount();
        setTopData();
    }

    public int getParticipantsThisMonth() { return this.participantsThisMonth; }
    public int getHostsThisMonth() { return this.hostsThisMonth; }
    public int getParticipantsAllTime() { return this.participantsAllTime; }
    public int getHostsAllTime() { return this.hostsAllTime; }
    public int getRunsAllTime() { return this.runsAllTime; }
    public int getRunsThisMonth() { return this.runsThisMonth; }
    public String getTopHostAllTime() { return topHostAllTime; }
    public String getHostWithMostAllTime() { return hostWithMostAllTime; }
    public String getTopParticipantAllTime() { return topParticipantAllTime; }
    public String getTopHostMonthly() { return topHostMonthly; }
    public String getHostWithMostMonthly() { return hostWithMostMonthly; }
    public String getTopParticipantMonthly() { return topParticipantMonthly; }

    //========== SQL Database GET requests
    private void setParticipationCount() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = BOT.getDatabase().getConnection();
            ps = con.prepareStatement("SELECT COUNT(DISTINCT uuid) AS total_participants, " +
                    "COUNT(DISTINCT host_id) AS total_hosts, " +
                    "(SELECT COUNT(uuid) FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` WHERE uuid=host_id) AS total_runs, " +
                    "(SELECT COUNT(uuid) FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` WHERE uuid=host_id AND DATE_FORMAT(date, '%Y-%m-01') = DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01')) AS monthly_runs, " +
                    "(SELECT COUNT(DISTINCT uuid) FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` WHERE DATE_FORMAT(date, '%Y-%m-01') = DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01')) AS monthly_participants, " +
                    "(SELECT COUNT(DISTINCT host_id) FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` WHERE DATE_FORMAT(date, '%Y-%m-01') = DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01')) AS monthly_hosts " +
                    "FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "`");
            rs = ps.executeQuery();
            if (rs.next()) {
                this.participantsAllTime = rs.getInt("total_participants");
                this.participantsThisMonth = rs.getInt("monthly_participants");
                this.hostsAllTime = rs.getInt("total_hosts");
                this.hostsThisMonth = rs.getInt("monthly_hosts");
                this.runsAllTime = rs.getInt("total_runs");
                this.runsThisMonth = rs.getInt("monthly_runs");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            BOT.getDatabase().closeQuietly(ps, rs, con);
        }

    }

    private void setTopData() { // I need to eventually refactor this to not be 6 different db queries, but I'll worry about that when/if performance is an issue
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Member member;

        try {
            int count = 0;
            con = BOT.getDatabase().getConnection();
            //=== Top Host All Time
            ps = con.prepareStatement("SELECT host_id, COUNT(uuid) AS runs " +
                    "FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` " +
                    "WHERE uuid = host_id " +
                    "GROUP BY host_id " +
                    "ORDER BY runs DESC " +
                    "LIMIT 3");
            rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                member = this.event.getGuild().getMemberById(rs.getString("host_id"));
                if (count>1) { this.topHostAllTime += "\n"; }
                if (member != null) { this.topHostAllTime += "**#" + count + ".** " + Responses.memberName(member) + " (" + rs.getString("runs") + ")"; }
            }
            //== Top Host This Month
            count = 0;
            ps = con.prepareStatement("SELECT host_id, COUNT(uuid) AS runs " +
                    "FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` " +
                    "WHERE uuid = host_id " +
                    "AND DATE_FORMAT(date, '%Y-%m-01') = DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01')" +
                    "GROUP BY host_id " +
                    "ORDER BY runs DESC " +
                    "LIMIT 3");
            rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                member = this.event.getGuild().getMemberById(rs.getString("host_id"));
                if (count>1) { this.topHostMonthly += "\n"; }
                if (member != null) { this.topHostMonthly += "**#" + count + ".** " + Responses.memberName(member) + " (" + rs.getString("runs") + ")"; }
            }
            //=== Host With Most Participants All Time
            count = 0;
            ps = con.prepareStatement("SELECT host_id, COUNT(DISTINCT uuid) AS runs " +
                    "FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` " +
                    "GROUP BY host_id " +
                    "ORDER BY runs DESC " +
                    "LIMIT 3");
            rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                member = this.event.getGuild().getMemberById(rs.getString("host_id"));
                if (count>1) { this.hostWithMostAllTime += "\n"; }
                if (member != null) { this.hostWithMostAllTime += "**#" + count + ".** " + Responses.memberName(member) + " (" + rs.getString("runs") + ")"; }
            }
            //== Host with Most Participants This Month
            count = 0;
            ps = con.prepareStatement("SELECT host_id, COUNT(DISTINCT uuid) AS runs " +
                    "FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` " +
                    "WHERE DATE_FORMAT(date, '%Y-%m-01') = DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01')" +
                    "GROUP BY host_id " +
                    "ORDER BY runs DESC " +
                    "LIMIT 3");
            rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                member = this.event.getGuild().getMemberById(rs.getString("host_id"));
                if (count>1) { this.hostWithMostMonthly += "\n"; }
                if (member != null) { this.hostWithMostMonthly += "**#" + count + ".** " + Responses.memberName(member) + " (" + rs.getString("runs") + ")"; }
            }
            //=== Top Participant All Time
            count = 0;
            ps = con.prepareStatement("SELECT uuid, COUNT(uuid) AS runs " +
                    "FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` " +
                    "WHERE uuid != host_id " +
                    "GROUP BY uuid " +
                    "ORDER BY runs DESC " +
                    "LIMIT 3");
            rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                member = this.event.getGuild().getMemberById(rs.getString("uuid"));
                if (count>1) { this.topParticipantAllTime += "\n"; }
                if (member != null) { this.topParticipantAllTime += "**#" + count + ".** " + Responses.memberName(member) + " (" + rs.getString("runs") + ")"; }
            }
            //== Top Participant This Month
            count = 0;
            ps = con.prepareStatement("SELECT uuid, COUNT(uuid) AS runs " +
                    "FROM `" + Environment.SQL_RUN_TRACKER_TABLE + "` " +
                    "WHERE uuid != host_id " +
                    "AND DATE_FORMAT(date, '%Y-%m-01') = DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01')" +
                    "GROUP BY uuid " +
                    "ORDER BY runs DESC " +
                    "LIMIT 3");
            rs = ps.executeQuery();
            while (rs.next()) {
                count++;
                member = this.event.getGuild().getMemberById(rs.getString("uuid"));
                if (count>1) { this.topParticipantMonthly += "\n"; }
                if (member != null) { this.topParticipantMonthly += "**#" + count + ".** " + Responses.memberName(member) + " (" + rs.getString("runs") + ")"; }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            BOT.getDatabase().closeQuietly(ps, rs, con);
        }

    }
}
