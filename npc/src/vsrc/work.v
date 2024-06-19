module filiter(
    input clk,
    input rst,
    input in,
    output out
);
    reg in_cache, out_cache;
    reg [9:0] counter;

    always@(posedge clk) begin
        if(rst) begin
            in_cache <= 0;
            out_cache <= 0;
            counter <= 0;
        end else begin
            in_cache <= in;

            if(in_cache == in) begin
                counter <= counter + 1;
                if(counter >= 10'd1000) begin 
                    out_cache <= in;
                    counter <= 0;
                end
            end else begin
                counter <= 0;
            end
            
        end
    end

    assign out = out_cache;

endmodule

module work(
    input clk,
    input rst,
    output [15:0] mem_store
);

    wire f_clk, f_rst;
    filiter clk_filiter(clk, rst, clk, f_clk);
    filiter rst_filiter(clk, rst, rst, f_rst);

    MyRAM RAM(
        .clock(f_clk),
        .reset(f_rst),
        
    )

endmodule