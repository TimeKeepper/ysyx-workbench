module ps2(
    input clock,
    input reset,
    input ps2_clk,
    input ps2_data,
    output reg[31:0] keycode,
    output reg valid
);

    reg pre_ps2_clk = 0;
    reg [7:0]counter_byte = 0;
    reg pre_flag_byte = 0;
    reg flag_byte = 0;
    reg [7:0]datacur = 0;
    reg [2:0] byte_num = 0;

    always@(posedge clock){
        if(reset) begin
            pre_ps2_clk <= 1'b0;
            keycode <= 8'h00;
        end

        pre_ps2_clk <= ps2_clk;
        pre_flag_byte <= flag_byte;

        if(pre_flag_byte == 1'b1 && flag_byte == 1'b0) begin
            byte_num <= byte_num + 1;
            keycode  <= {keycode[23:0], datacur};
        end

        if(byte_num == 3) begin
            valid <= 1'b1;
        end

        if(pre_ps2_clk == 1'b1 && ps2_clk == 1'b0) begin
            case(counter_byte)
                0:;
                1: datacur[0] <= ps2_data;
                2: datacur[1] <= ps2_data;
                3: datacur[2] <= ps2_data;
                4: datacur[3] <= ps2_data;
                5: datacur[4] <= ps2_data;
                6: datacur[5] <= ps2_data;
                7: datacur[6] <= ps2_data;
                8: datacur[7] <= ps2_data;
                9: flag_byte  <= 1'b1;
                10: flag_byte <= 1'b0;
            endcase

            if(counter_byte<=9) counter_byte<=counter_byte+1;
            else if(counter_byte==10) counter_byte<=0;
        end
    }

endmodule