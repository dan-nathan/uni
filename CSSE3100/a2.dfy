method mult2(X:	int, Y:	int) returns (r: int)	
    requires Y > 0 || X == 0
    ensures r == X * Y
{
    var x, y, c := X, Y, 0;
    while y > 1
        invariant x * y + c == X * Y
        invariant y > 0 || x == 0
        decreases y
    {
        if y % 2 == 0 {
            y := y / 2;
        } else {
            y := (y - 1) / 2;
            c := c + x;
        }
        simplify2(x, y);
        x := 2 * x;
    }    
    r := c + x;
}

lemma simplify2(x: int, y: int)
    ensures y % 2 == 1 ==> 2 * x * ((y - 1) / 2) + x == x * y
{
    if (y % 2 == 1) {
        simplify(y);
    }
}    

lemma simplify(y: int)
    ensures y % 2 == 1 ==> 2 * ((y - 1) / 2) == y - 1
{}    

function poly (x: int): int { x*x*x - 6*x*x + 9*x }

method smallestX (N: int) returns (x: int)
    requires N >= 0
    ensures N <= poly(x) 
    ensures N > poly(x-1)
{
    // The possible cases are x == 0, x == 1, x >= 5. Check the 1st 2 cases separately
    if (N == 0) {
        x := 0;
    } else if (N <= 4) {
        x := 1;
    } else {
        x := 5;
        var y := 20;
        var z := 34;
        var w := 24;
        while (y < N) 
            decreases N - poly(x) // x^3 - 6x^2 + 9x is increasing for x > 3
            invariant x >= 5
            invariant N > poly(x-1)
            invariant y == poly(x)
            invariant z == 3*x*x - 9*x + 4
            invariant w == 6*x - 6
        {
            calc {
                poly(x - 1);     
            == (x-1)*(x-1)*(x-1) - 6*(x-1)*(x-1) + 9*(x-1);
            == (x*x*x - 3*x*x + 3*x - 1) + (-6*x*x + 12*x - 6) + (9*x - 9);
            == x*x*x - 9*x*x + 24*x - 16;
            == (x*x*x - 9*x*x + 24*x - 16) + (3*5*x - 15*x);
            <= (x*x*x - 9*x*x + 24*x - 16) + (3*x*x - 15*x);
            < (x*x*x - 9*x*x + 24*x - 16) + (3*x*x - 15*x + 16);
            == x*x*x - 6*x*x + 9*x; 
            == poly(x);
            }
            calc {
                y + z;
            == (x*x*x - 6*x*x + 9*x) + (3*x*x - 9*x + 4);
            == x*x*x - 3*x*x + 4;
            }
            calc {
                z + w;
            == (3*x*x - 9*x + 4) + (6*x - 6);
            == 3*x*x - 3*x - 2;  
            }
            calc {
                w + 6;
            == (6*x - 6) + 6;
            }
            y, z, w := y + z, z + w, w + 6;
            calc {
                y;
            == x*x*x - 3*x*x + 4;
            == (x*x*x + 3*x*x + 3*x + 1) + (-6*x*x - 12*x - 6) + (9*x + 9);
            == (x+1)*(x*x + 2*x + 1) - 6*(x*x + 2*x + 1) + 9*(x+1);
            == (x+1)*(x+1)*(x+1) - 6*(x+1)*(x+1) + 9*(x+1);
            }
            calc {
                z;
            == 3*x*x - 3*x - 2;
            == (3*x*x + 6*x + 3) - (9*x + 9) + 4;
            == 3*(x*x + 2*x + 1) - 9*(x+1) + 4;
            == 3*(x+1)*(x+1) - 9*(x+1) + 4;
            }
            calc {
                w;
            == (6*x - 6) + 6;
            == (6*x + 6) - 6;
            == 6*(x+1) - 6;
            }
            x := x + 1;
        }
    } 
}